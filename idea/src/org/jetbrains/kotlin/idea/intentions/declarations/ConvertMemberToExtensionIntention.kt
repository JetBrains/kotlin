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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.setReceiverType
import org.jetbrains.kotlin.idea.quickfix.moveCaret
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*

public class ConvertMemberToExtensionIntention : JetSelfTargetingRangeIntention<JetCallableDeclaration>(javaClass(), "Convert member to extension") {
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

        val typeParameterList = newTypeParameterList(element)

        val project = element.getProject()
        val psiFactory = JetPsiFactory(element)

        val extension = file.addAfter(element, outermostParent) as JetCallableDeclaration
        file.addAfter(psiFactory.createNewLine(), outermostParent)
        file.addAfter(psiFactory.createNewLine(), outermostParent)
        element.delete()

        extension.setReceiverType(containingClass.getDefaultType())

        if (typeParameterList != null) {
            if (extension.getTypeParameterList() != null) {
                extension.getTypeParameterList()!!.replace(typeParameterList)
            }
            else {
                extension.addBefore(typeParameterList, extension.getReceiverTypeReference())
                extension.addBefore(psiFactory.createWhiteSpace(), extension.getReceiverTypeReference())
            }
        }

        extension.getModifierList()?.getModifier(JetTokens.PROTECTED_KEYWORD)?.delete()
        extension.getModifierList()?.getModifier(JetTokens.ABSTRACT_KEYWORD)?.delete()

        var bodyToSelect: JetExpression? = null

        fun selectBody(declaration: JetDeclarationWithBody) {
            if (bodyToSelect == null) {
                val body = declaration.getBodyExpression()
                bodyToSelect = if (body is JetBlockExpression) body.getStatements().single() as JetExpression else body
            }
        }

        when (extension) {
            is JetFunction -> {
                if (!extension.hasBody()) {
                    extension.add(psiFactory.createFunctionBody(THROW_UNSUPPORTED_OPERATION_EXCEPTION))
                    selectBody(extension)
                }
            }

            is JetProperty -> {
                val templateProperty = psiFactory.createDeclaration<JetProperty>("var v: Any\nget()=$THROW_UNSUPPORTED_OPERATION_EXCEPTION\nset(value){$THROW_UNSUPPORTED_OPERATION_EXCEPTION}")
                val templateGetter = templateProperty.getGetter()!!
                val templateSetter = templateProperty.getSetter()!!

                var getter = extension.getGetter()
                if (getter == null) {
                    getter = extension.addAfter(templateGetter, extension.getTypeReference()) as JetPropertyAccessor
                    extension.addBefore(psiFactory.createNewLine(), getter)
                    selectBody(getter)
                }
                else if (!getter.hasBody()) {
                    getter = getter.replace(templateGetter) as JetPropertyAccessor
                    selectBody(getter)
                }

                if (extension.isVar()) {
                    var setter = extension.getSetter()
                    if (setter == null) {
                        setter = extension.addAfter(templateSetter, getter) as JetPropertyAccessor
                        extension.addBefore(psiFactory.createNewLine(), setter)
                        selectBody(setter)
                    }
                    else if (!setter.hasBody()) {
                        setter = setter.replace(templateSetter) as JetPropertyAccessor
                        selectBody(setter)
                    }
                }
            }
        }

        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument())

        if (bodyToSelect != null) {
            val range = bodyToSelect!!.getTextRange()
            editor.moveCaret(range.getStartOffset(), ScrollType.CENTER)
            editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset())
        }
        else {
            editor.moveCaret(extension.getTextOffset(), ScrollType.CENTER)
        }
    }

    private val THROW_UNSUPPORTED_OPERATION_EXCEPTION = "throw UnsupportedOperationException()"

    private fun newTypeParameterList(member: JetCallableDeclaration): JetTypeParameterList? {
        val classElement = member.getParent().getParent() as JetClass
        val classParams = classElement.getTypeParameters()
        if (classParams.isEmpty()) return null
        val allTypeParameters = classParams + member.getTypeParameters()
        val text = allTypeParameters.map { it.getText() }.joinToString(",", "<", ">")
        return JetPsiFactory(member).createDeclaration<JetFunction>("fun $text foo()").getTypeParameterList()
    }
}
