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

package org.jetbrains.kotlin.idea.core.overrideImplement

import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.MemberChooser
import com.intellij.lang.LanguageCodeInsightActionHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.quickfix.moveCaretIntoGeneratedElement
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.renderer.*
import java.util.ArrayList

public abstract class OverrideImplementMethodsHandler : LanguageCodeInsightActionHandler {

    public fun collectMethodsToGenerate(classOrObject: JetClassOrObject): Collection<OverrideMemberChooserObject> {
        val descriptor = classOrObject.resolveToDescriptor() as? ClassDescriptor ?: return emptySet()
        return collectMethodsToGenerate(descriptor, classOrObject.project)
    }

    protected abstract fun collectMethodsToGenerate(descriptor: ClassDescriptor, project: Project): Collection<OverrideMemberChooserObject>

    private fun showOverrideImplementChooser(project: Project, members: Array<OverrideMemberChooserObject>): MemberChooser<OverrideMemberChooserObject>? {
        val chooser = MemberChooser(members, true, true, project)
        chooser.title = getChooserTitle()
        chooser.show()
        if (chooser.exitCode != DialogWrapper.OK_EXIT_CODE) return null
        return chooser
    }

    protected abstract fun getChooserTitle(): String

    override fun isValidFor(editor: Editor, file: PsiFile): Boolean {
        if (file !is JetFile) return false
        val elementAtCaret = file.findElementAt(editor.caretModel.offset)
        val classOrObject = elementAtCaret?.getNonStrictParentOfType<JetClassOrObject>()
        return classOrObject != null
    }

    protected abstract fun getNoMethodsFoundHint(): String

    public fun invoke(project: Project, editor: Editor, file: PsiFile, implementAll: Boolean) {
        val elementAtCaret = file.findElementAt(editor.caretModel.offset)
        val classOrObject = elementAtCaret?.getNonStrictParentOfType<JetClassOrObject>()!!

        val members = collectMethodsToGenerate(classOrObject)
        if (members.isEmpty() && !implementAll) {
            HintManager.getInstance().showErrorHint(editor, getNoMethodsFoundHint())
            return
        }

        val selectedElements = if (implementAll) {
            members
        }
        else {
            val chooser = showOverrideImplementChooser(project, members.toTypedArray()) ?: return
            chooser.selectedElements ?: return
        }
        if (selectedElements.isEmpty()) return

        PsiDocumentManager.getInstance(project).commitAllDocuments()

        generateMethods(editor, classOrObject, selectedElements)
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) = invoke(project, editor, file, false)

    override fun startInWriteAction(): Boolean = false

    companion object {
        private val OVERRIDE_RENDERER = DescriptorRenderer.withOptions {
            renderDefaultValues = false
            modifiers = setOf(DescriptorRendererModifier.OVERRIDE)
            withDefinedIn = false
            nameShortness = NameShortness.SOURCE_CODE_QUALIFIED
            overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OVERRIDE
            unitReturnType = false
            typeNormalizer = IdeDescriptorRenderers.APPROXIMATE_FLEXIBLE_TYPES
        }

        public fun generateMethods(editor: Editor, classOrObject: JetClassOrObject, selectedElements: Collection<OverrideMemberChooserObject>) {
            runWriteAction {
                val body = classOrObject.getOrCreateBody()

                var afterAnchor = findInsertAfterAnchor(editor, body) ?: return@runWriteAction

                var firstGenerated: PsiElement? = null

                val elementsToCompact = ArrayList<JetElement>()
                for (element in generateOverridingMembers(selectedElements, classOrObject)) {
                    val added = body.addAfter(element, afterAnchor)

                    if (firstGenerated == null) {
                        firstGenerated = added
                    }

                    afterAnchor = added
                    elementsToCompact.add(added as JetElement)
                }

                ShortenReferences.Companion.DEFAULT.process(elementsToCompact)

                if (firstGenerated == null) return@runWriteAction

                val project = classOrObject.project
                val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(firstGenerated)

                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

                val element = pointer.element
                if (element != null) {
                    moveCaretIntoGeneratedElement(editor, element)
                }

            }
        }

        private fun findInsertAfterAnchor(editor: Editor, body: JetClassBody): PsiElement? {
            val afterAnchor = body.lBrace ?: return null

            val offset = editor.caretModel.offset
            val offsetCursorElement = PsiTreeUtil.findFirstParent(body.containingFile.findElementAt(offset)) {
                it.parent == body
            }

            if (offsetCursorElement is PsiWhiteSpace) {
                return removeAfterOffset(offset, offsetCursorElement)
            }

            if (offsetCursorElement != null && offsetCursorElement != body.rBrace) {
                return offsetCursorElement
            }

            return afterAnchor
        }

        private fun removeAfterOffset(offset: Int, whiteSpace: PsiWhiteSpace): PsiElement {
            val spaceNode = whiteSpace.node
            if (spaceNode.textRange.contains(offset)) {
                var beforeWhiteSpaceText = spaceNode.text.substring(0, offset - spaceNode.startOffset)
                if (!StringUtil.containsLineBreak(beforeWhiteSpaceText)) {
                    // Prevent insertion on same line
                    beforeWhiteSpaceText += "\n"
                }

                val factory = JetPsiFactory(whiteSpace.project)

                val insertAfter = whiteSpace.prevSibling
                whiteSpace.delete()

                val beforeSpace = factory.createWhiteSpace(beforeWhiteSpaceText)
                insertAfter.parent.addAfter(beforeSpace, insertAfter)

                return insertAfter.nextSibling
            }

            return whiteSpace
        }

        private fun generateOverridingMembers(selectedElements: Collection<OverrideMemberChooserObject>,
                                              classOrObject: JetClassOrObject): List<JetElement> {
            val overridingMembers = ArrayList<JetElement>()
            for (selectedElement in selectedElements) {
                val descriptor = selectedElement.immediateSuper
                when (descriptor) {
                    is SimpleFunctionDescriptor -> overridingMembers.add(overrideFunction(classOrObject, descriptor))
                    is PropertyDescriptor -> overridingMembers.add(overrideProperty(classOrObject, descriptor))
                    else -> error("Unknown member to override: $descriptor")
                }
            }
            return overridingMembers
        }

        private fun overrideProperty(classOrObject: JetClassOrObject, descriptor: PropertyDescriptor): JetElement {
            val newDescriptor = descriptor.copy(descriptor.containingDeclaration, Modality.OPEN, descriptor.visibility,
                                                descriptor.kind, /* copyOverrides = */ true) as PropertyDescriptor
            newDescriptor.addOverriddenDescriptor(descriptor)

            val body = StringBuilder()
            body.append("\nget()")
            body.append(" = ")
            body.append(generateUnsupportedOrSuperCall(classOrObject, descriptor))
            if (descriptor.isVar) {
                body.append("\nset(value) {}")
            }
            return JetPsiFactory(classOrObject.project).createProperty(OVERRIDE_RENDERER.render(newDescriptor) + body)
        }

        private fun overrideFunction(classOrObject: JetClassOrObject, descriptor: FunctionDescriptor): JetNamedFunction {
            val newDescriptor = descriptor.copy(descriptor.containingDeclaration, Modality.OPEN, descriptor.visibility,
                                                descriptor.kind, /* copyOverrides = */ true)
            newDescriptor.addOverriddenDescriptor(descriptor)

            val returnType = descriptor.returnType
            val returnsNotUnit = returnType != null && !KotlinBuiltIns.isUnit(returnType)
            val isAbstract = descriptor.modality == Modality.ABSTRACT

            val delegation = generateUnsupportedOrSuperCall(classOrObject, descriptor)

            val body = "{" + (if (returnsNotUnit && !isAbstract) "return " else "") + delegation + "}"

            return JetPsiFactory(classOrObject.project).createFunction(OVERRIDE_RENDERER.render(newDescriptor) + body)
        }

        private fun generateUnsupportedOrSuperCall(classOrObject: JetClassOrObject, descriptor: CallableMemberDescriptor): String {
            val isAbstract = descriptor.modality == Modality.ABSTRACT
            if (isAbstract) {
                return "throw UnsupportedOperationException()"
            }
            else {
                val builder = StringBuilder()
                builder.append("super")
                if (classOrObject.getDelegationSpecifiers().size() > 1) {
                    builder.append("<").append(descriptor.containingDeclaration.escapedName()).append(">")
                }
                builder.append(".").append(descriptor.escapedName())

                if (descriptor is FunctionDescriptor) {
                    val paramTexts = descriptor.valueParameters.map {
                        val renderedName = it.escapedName()
                        if (it.varargElementType != null) "*$renderedName" else renderedName
                    }
                    paramTexts.joinTo(builder, prefix="(", postfix=")")
                }

                return builder.toString()
            }
        }

        private fun DeclarationDescriptor.escapedName() = name.render()
    }
}
