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
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Function
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingRangeIntention
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*

public class ConvertMemberToExtension : JetSelfTargetingRangeIntention<JetCallableDeclaration>(javaClass(), "Convert to extension") {
    override fun applicabilityRange(element: JetCallableDeclaration): TextRange? {
        val classBody = element.getParent() as? JetClassBody ?: return null
        if (classBody.getParent() !is JetClass) return null
        if (element.getReceiverTypeReference() != null) return null
        if (element.hasModifier(JetTokens.OVERRIDE_KEYWORD)) return null
        when (element) {
            is JetProperty -> if (element.hasInitializer()) return null
            is JetSecondaryConstructor -> return null
        }
        return (element.getNameIdentifier() ?: return null).getTextRange()
    }

    override fun applyTo(element: JetCallableDeclaration, editor: Editor) {
        val descriptor = element.resolveToDescriptor()
        val containingClass = descriptor.getContainingDeclaration() as ClassDescriptor

        val file = element.getContainingJetFile()
        val outermostParent = JetPsiUtil.getOutermostParent(element, file, false)

        val receiver = containingClass.getDefaultType().toString() + "."
        val name = element.getNameIdentifier()!!.getText()

        val valueParameterList = element.getValueParameterList()
        val returnTypeRef = element.getTypeReference()

        val extensionText = modifiers(element) +
                            memberType(element) +
                            " " +
                            typeParameters(element) +
                            receiver +
                            name +
                            (if (valueParameterList == null) "" else valueParameterList.getText()) +
                            (if (returnTypeRef != null) ": " + returnTypeRef.getText() else "") +
                            body(element)

        val psiFactory = JetPsiFactory(element)
        val extension = psiFactory.createDeclaration<JetDeclaration>(extensionText)

        val added = file.addAfter(extension, outermostParent)
        file.addAfter(psiFactory.createNewLine(), outermostParent)
        file.addAfter(psiFactory.createNewLine(), outermostParent)
        element.delete()

        CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement<PsiElement>(added)

        val caretAnchor = added.getText().indexOf(CARET_ANCHOR)
        if (caretAnchor >= 0) {
            val caretOffset = added.getTextRange().getStartOffset() + caretAnchor
            val anchor = PsiTreeUtil.findElementOfClassAtOffset(file, caretOffset, javaClass<JetSimpleNameExpression>(), false)
            if (anchor != null && CARET_ANCHOR == anchor.getReferencedName()) {
                val throwException = psiFactory.createExpression(THROW_UNSUPPORTED_OPERATION_EXCEPTION)
                val replaced = anchor.replace(throwException)
                val range = replaced.getTextRange()
                editor.getCaretModel().moveToOffset(range.getStartOffset())
                editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset())
            }
        }
    }

    private val CARET_ANCHOR = "____CARET_ANCHOR____"
    private val THROW_UNSUPPORTED_OPERATION_EXCEPTION = " throw UnsupportedOperationException()"

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
        return getter!!.getText()
    }

    private fun setter(property: JetProperty, allowCaretAnchor: Boolean): String {
        if (!property.isVar()) return ""
        val setter = property.getSetter()
        if (synthesizeBody(setter)) return "set(value) {" + (if (allowCaretAnchor) CARET_ANCHOR else THROW_UNSUPPORTED_OPERATION_EXCEPTION) + "}"
        return setter!!.getText()
    }

    private fun synthesizeBody(getter: JetPropertyAccessor?): Boolean {
        return getter == null || getter.getBodyExpression() == null
    }
}
