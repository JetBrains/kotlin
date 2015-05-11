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

package org.jetbrains.kotlin.idea.intentions.declarations

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Function
import com.intellij.util.IncorrectOperationException
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR

public class ConvertMemberToExtension : BaseIntentionAction() {

    override fun getText(): String {
        return getFamilyName()
    }

    override fun getFamilyName(): String {
        return JetBundle.message("convert.to.extension")
    }

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        val declaration = getTarget(editor, file)
        if (declaration is JetProperty) {
            if (declaration.hasInitializer()) return false
        }
        return declaration != null && declaration !is JetSecondaryConstructor && declaration.getParent() is JetClassBody && declaration.getParent().getParent() is JetClass && declaration.getReceiverTypeReference() == null
    }

    throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val member = getTarget(editor, file)
        assert(member != null, "Must be checked by isAvailable")

        val bindingContext = member!!.analyzeFully()
        val memberDescriptor = bindingContext.get<PsiElement, DeclarationDescriptor>(DECLARATION_TO_DESCRIPTOR, member) ?: return

        val containingClass = memberDescriptor.getContainingDeclaration()
        assert(containingClass is ClassDescriptor) { "Members must be contained in classes: \n" + "Descriptor: " + memberDescriptor + "\n" + "Declaration & context: " + member.getParent().getParent().getText() }

        val outermostParent = JetPsiUtil.getOutermostParent(member, file, false)

        val receiver = (containingClass as ClassDescriptor).getDefaultType().toString() + "."
        val identifier = member.getNameIdentifier()
        val name = if (identifier == null) "" else identifier.getText()

        val valueParameterList = member.getValueParameterList()
        val returnTypeRef = member.getTypeReference()

        val extensionText = modifiers(member) + memberType(member) + " " + typeParameters(member) + receiver + name + (if (valueParameterList == null) "" else valueParameterList.getText()) + (if (returnTypeRef != null) ": " + returnTypeRef.getText() else "") + body(member)

        val psiFactory = JetPsiFactory(member)
        val extension = psiFactory.createDeclaration<JetDeclaration>(extensionText)

        val added = file.addAfter(extension, outermostParent)
        file.addAfter(psiFactory.createNewLine(), outermostParent)
        file.addAfter(psiFactory.createNewLine(), outermostParent)
        member.delete()

        CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement<PsiElement>(added)

        val caretAnchor = added.getText().indexOf(CARET_ANCHOR)
        if (caretAnchor >= 0) {
            val caretOffset = added.getTextRange().getStartOffset() + caretAnchor
            val anchor = PsiTreeUtil.findElementOfClassAtOffset<JetSimpleNameExpression>(file, caretOffset, javaClass<JetSimpleNameExpression>(), false)
            if (anchor != null && CARET_ANCHOR == anchor.getReferencedName()) {
                val throwException = psiFactory.createExpression(THROW_UNSUPPORTED_OPERATION_EXCEPTION)
                val replaced = anchor.replace(throwException)
                val range = replaced.getTextRange()
                editor.getCaretModel().moveToOffset(range.getStartOffset())
                editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset())
            }
        }
    }

    companion object {

        public val CARET_ANCHOR: String = "____CARET_ANCHOR____"
        public val THROW_UNSUPPORTED_OPERATION_EXCEPTION: String = " throw UnsupportedOperationException()"

        private fun getTarget(editor: Editor, file: PsiFile): JetCallableDeclaration? {
            val element = file.findElementAt(editor.getCaretModel().getOffset())
            return PsiTreeUtil.getParentOfType<JetCallableDeclaration>(element, javaClass<JetCallableDeclaration>(), false, javaClass<JetExpression>())
        }

        private fun memberType(member: JetCallableDeclaration): String {
            if (member is JetFunction) {
                return "fun"
            }
            return (member as JetProperty).getValOrVarNode().getText()
        }

        private fun modifiers(member: JetCallableDeclaration): String {
            val modifierList = member.getModifierList() ?: return ""
            for (modifierType in JetTokens.VISIBILITY_MODIFIERS.getTypes()) {
                val modifier = modifierList.getModifier(modifierType as JetModifierKeywordToken)
                if (modifier != null) {
                    return if (modifierType == JetTokens.PROTECTED_KEYWORD) "" else modifier.getText() + " "
                }
            }
            return ""
        }

        private fun typeParameters(member: JetCallableDeclaration): String {
            val classElement = member.getParent().getParent()
            assert(classElement is JetClass) { "Must be checked in isAvailable: " + classElement.getText() }

            val allTypeParameters = ContainerUtil.concat<JetTypeParameter>((classElement as JetClass).getTypeParameters(), member.getTypeParameters())
            if (allTypeParameters.isEmpty()) return ""
            return "<" + StringUtil.join<JetTypeParameter>(allTypeParameters, object : Function<JetTypeParameter, String> {
                override fun `fun`(parameter: JetTypeParameter): String {
                    return parameter.getText()
                }
            }, ", ") + "> "
        }

        private fun body(member: JetCallableDeclaration): String {
            if (member is JetProperty) {
                return "\n" + getter(member) + "\n" + setter(member, !synthesizeBody(member.getGetter()))
            }
            else if (member is JetFunction) {
                val bodyExpression = member.getBodyExpression() ?: return "{" + CARET_ANCHOR + "}"
                if (!member.hasBlockBody()) return " = " + bodyExpression.getText()
                return bodyExpression.getText()
            }
            else {
                return ""
            }
        }

        private fun getter(property: JetProperty): String {
            val getter = property.getGetter()
            if (synthesizeBody(getter)) return "get() = " + CARET_ANCHOR
            return getter.getText()
        }

        private fun setter(property: JetProperty, allowCaretAnchor: Boolean): String {
            if (!property.isVar()) return ""
            val setter = property.getSetter()
            if (synthesizeBody(setter)) return "set(value) {" + (if (allowCaretAnchor) CARET_ANCHOR else THROW_UNSUPPORTED_OPERATION_EXCEPTION) + "}"
            return setter.getText()
        }

        private fun synthesizeBody(getter: JetPropertyAccessor?): Boolean {
            return getter == null || getter.getBodyExpression() == null
        }
    }
}
