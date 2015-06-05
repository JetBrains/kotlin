/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.core.codeInsight

import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.MemberChooser
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageCodeInsightActionHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.quickfix.*
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.NameShortness
import org.jetbrains.kotlin.types.JetType

import java.util.ArrayList
import java.util.Collections

import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

public abstract class OverrideImplementMethodsHandler : LanguageCodeInsightActionHandler {

    public fun collectMethodsToGenerate(classOrObject: JetClassOrObject): Set<CallableMemberDescriptor> {
        val descriptor = classOrObject.resolveToDescriptor()
        if (descriptor is ClassDescriptor) {
            return collectMethodsToGenerate(descriptor)
        }
        return emptySet()
    }

    protected abstract fun collectMethodsToGenerate(descriptor: ClassDescriptor): Set<CallableMemberDescriptor>

    private fun showOverrideImplementChooser(project: Project, members: Array<DescriptorClassMember>): MemberChooser<DescriptorClassMember>? {
        val chooser = MemberChooser(members, true, true, project)
        chooser.setTitle(getChooserTitle())
        chooser.show()
        if (chooser.getExitCode() != DialogWrapper.OK_EXIT_CODE) return null
        return chooser
    }

    protected abstract fun getChooserTitle(): String

    override fun isValidFor(editor: Editor, file: PsiFile): Boolean {
        if (file !is JetFile) {
            return false
        }
        val elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset())
        val classOrObject = elementAtCaret?.getNonStrictParentOfType<JetClassOrObject>()
        return classOrObject != null
    }

    protected abstract fun getNoMethodsFoundHint(): String

    public fun invoke(project: Project, editor: Editor, file: PsiFile, implementAll: Boolean) {
        val elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset())
        val classOrObject = elementAtCaret?.getNonStrictParentOfType<JetClassOrObject>()!!

        val missingImplementations = collectMethodsToGenerate(classOrObject)
        if (missingImplementations.isEmpty() && !implementAll) {
            HintManager.getInstance().showErrorHint(editor, getNoMethodsFoundHint())
            return
        }
        val members = membersFromDescriptors(file as JetFile, missingImplementations)

        val selectedElements: List<DescriptorClassMember>?
        if (implementAll) {
            selectedElements = members
        }
        else {
            val chooser = showOverrideImplementChooser(project, members.toTypedArray())

            if (chooser == null) {
                return
            }

            selectedElements = chooser.getSelectedElements()
            if (selectedElements == null || selectedElements.isEmpty()) return
        }

        PsiDocumentManager.getInstance(project).commitAllDocuments()

        generateMethods(editor, classOrObject, selectedElements)
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) = invoke(project, editor, file, false)

    override fun startInWriteAction(): Boolean = false

    companion object {
        private val OVERRIDE_RENDERER = DescriptorRenderer.withOptions {
            renderDefaultValues = false
            modifiers = setOf(DescriptorRenderer.Modifier.OVERRIDE)
            withDefinedIn = false
            nameShortness = NameShortness.SOURCE_CODE_QUALIFIED
            overrideRenderingPolicy = DescriptorRenderer.OverrideRenderingPolicy.RENDER_OVERRIDE
            unitReturnType = false
            typeNormalizer = IdeDescriptorRenderers.APPROXIMATE_FLEXIBLE_TYPES
        }

        private val LOG = Logger.getInstance(javaClass<OverrideImplementMethodsHandler>().getCanonicalName())

        public fun membersFromDescriptors(file: JetFile, missingImplementations: Iterable<CallableMemberDescriptor>): List<DescriptorClassMember> {
            val members = ArrayList<DescriptorClassMember>()
            for (memberDescriptor in missingImplementations) {
                val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(file.getProject(), memberDescriptor)
                if (declaration == null) {
                    LOG.error("Can not find declaration for descriptor " + memberDescriptor)
                }
                else {
                    val member = DescriptorClassMember(declaration, memberDescriptor)
                    members.add(member)
                }
            }
            return members
        }

        public fun generateMethods(editor: Editor, classOrObject: JetClassOrObject, selectedElements: List<DescriptorClassMember>) {
            runWriteAction {
                var body = classOrObject.getBody()
                if (body == null) {
                    val psiFactory = JetPsiFactory(classOrObject)
                    classOrObject.add(psiFactory.createWhiteSpace())
                    body = classOrObject.add(psiFactory.createEmptyClassBody()) as JetClassBody
                }

                var afterAnchor = findInsertAfterAnchor(editor, body)

                if (afterAnchor == null) return@runWriteAction

                var firstGenerated: PsiElement? = null

                val elementsToCompact = ArrayList<JetElement>()
                for (element in generateOverridingMembers(selectedElements, classOrObject)) {
                    val added = body!!.addAfter(element, afterAnchor)

                    if (firstGenerated == null) {
                        firstGenerated = added
                    }

                    afterAnchor = added
                    elementsToCompact.add(added as JetElement)
                }

                ShortenReferences.DEFAULT.process(elementsToCompact)

                if (firstGenerated == null) return@runWriteAction

                val project = classOrObject.getProject()
                val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(firstGenerated)

                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument())

                val element = pointer.getElement()
                if (element != null) {
                    moveCaretIntoGeneratedElement(editor, element)
                }

            }
        }

        private fun findInsertAfterAnchor(editor: Editor, body: JetClassBody): PsiElement? {
            val afterAnchor = body.getLBrace()
            if (afterAnchor == null) return null

            val offset = editor.getCaretModel().getOffset()
            val offsetCursorElement = PsiTreeUtil.findFirstParent(body.getContainingFile().findElementAt(offset)) {
                it.getParent() == body
            }

            if (offsetCursorElement is PsiWhiteSpace) {
                return removeAfterOffset(offset, offsetCursorElement)
            }

            if (offsetCursorElement != null && offsetCursorElement != body.getRBrace()) {
                return offsetCursorElement
            }

            return afterAnchor
        }

        private fun removeAfterOffset(offset: Int, whiteSpace: PsiWhiteSpace): PsiElement {
            val spaceNode = whiteSpace.getNode()
            if (spaceNode.getTextRange().contains(offset)) {
                var beforeWhiteSpaceText = spaceNode.getText().substring(0, offset - spaceNode.getStartOffset())
                if (!StringUtil.containsLineBreak(beforeWhiteSpaceText)) {
                    // Prevent insertion on same line
                    beforeWhiteSpaceText += "\n"
                }

                val factory = JetPsiFactory(whiteSpace.getProject())

                val insertAfter = whiteSpace.getPrevSibling()
                whiteSpace.delete()

                val beforeSpace = factory.createWhiteSpace(beforeWhiteSpaceText)
                insertAfter.getParent().addAfter(beforeSpace, insertAfter)

                return insertAfter.getNextSibling()
            }

            return whiteSpace
        }

        private fun generateOverridingMembers(selectedElements: List<DescriptorClassMember>,
                                              classOrObject: JetClassOrObject): List<JetElement> {
            val overridingMembers = ArrayList<JetElement>()
            for (selectedElement in selectedElements) {
                val descriptor = selectedElement.getDescriptor()
                if (descriptor is SimpleFunctionDescriptor) {
                    overridingMembers.add(overrideFunction(classOrObject, descriptor))
                }
                else if (descriptor is PropertyDescriptor) {
                    overridingMembers.add(overrideProperty(classOrObject, descriptor))
                }
            }
            return overridingMembers
        }

        private fun overrideProperty(classOrObject: JetClassOrObject, descriptor: PropertyDescriptor): JetElement {
            val newDescriptor = descriptor.copy(descriptor.getContainingDeclaration(), Modality.OPEN, descriptor.getVisibility(),
                                                descriptor.getKind(), /* copyOverrides = */ true) as PropertyDescriptor
            newDescriptor.addOverriddenDescriptor(descriptor)

            val body = StringBuilder()
            body.append("\nget()")
            body.append(" = ")
            body.append(generateUnsupportedOrSuperCall(classOrObject, descriptor))
            if (descriptor.isVar()) {
                body.append("\nset(value) {}")
            }
            return JetPsiFactory(classOrObject.getProject()).createProperty(OVERRIDE_RENDERER.render(newDescriptor) + body)
        }

        private fun overrideFunction(classOrObject: JetClassOrObject, descriptor: FunctionDescriptor): JetNamedFunction {
            val newDescriptor = descriptor.copy(descriptor.getContainingDeclaration(), Modality.OPEN, descriptor.getVisibility(),
                                                descriptor.getKind(), /* copyOverrides = */ true)
            newDescriptor.addOverriddenDescriptor(descriptor)

            val returnType = descriptor.getReturnType()
            val returnsNotUnit = returnType != null && !KotlinBuiltIns.isUnit(returnType)
            val isAbstract = descriptor.getModality() == Modality.ABSTRACT

            val delegation = generateUnsupportedOrSuperCall(classOrObject, descriptor)

            val body = "{" + (if (returnsNotUnit && !isAbstract) "return " else "") + delegation + "}"

            return JetPsiFactory(classOrObject.getProject()).createFunction(OVERRIDE_RENDERER.render(newDescriptor) + body)
        }

        private fun generateUnsupportedOrSuperCall(classOrObject: JetClassOrObject, descriptor: CallableMemberDescriptor): String {
            val isAbstract = descriptor.getModality() == Modality.ABSTRACT
            if (isAbstract) {
                return "throw UnsupportedOperationException()"
            }
            else {
                val builder = StringBuilder()
                builder.append("super")
                if (classOrObject.getDelegationSpecifiers().size() > 1) {
                    builder.append("<").append(descriptor.getContainingDeclaration().escapedName()).append(">")
                }
                builder.append(".").append(descriptor.escapedName())

                if (descriptor is FunctionDescriptor) {
                    val paramTexts = descriptor.getValueParameters().map {
                        val renderedName = it.escapedName()
                        if (it.getVarargElementType() != null) "*$renderedName" else renderedName
                    }
                    paramTexts.joinTo(builder, prefix="(", postfix=")")
                }

                return builder.toString()
            }
        }

        fun DeclarationDescriptor.escapedName() =
                DescriptorRenderer.COMPACT.renderName(getName())
    }
}
