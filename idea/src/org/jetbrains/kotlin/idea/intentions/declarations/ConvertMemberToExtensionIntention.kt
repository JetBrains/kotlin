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

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.SmartList
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.utils.addIfNotNull

class ConvertMemberToExtensionIntention : SelfTargetingRangeIntention<KtCallableDeclaration>(KtCallableDeclaration::class.java, "Convert member to extension"), LowPriorityAction {
    override fun applicabilityRange(element: KtCallableDeclaration): TextRange? {
        val classBody = element.parent as? KtClassBody ?: return null
        if (classBody.parent !is KtClass) return null
        if (element.receiverTypeReference != null) return null
        if (element.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return null
        when (element) {
            is KtProperty -> if (element.hasInitializer()) return null
            is KtSecondaryConstructor -> return null
        }
        return (element.nameIdentifier ?: return null).textRange
    }

    //TODO: local class

    override fun applyTo(element: KtCallableDeclaration, editor: Editor?) {
        val descriptor = element.resolveToDescriptor()
        val containingClass = descriptor.containingDeclaration as ClassDescriptor

        val file = element.getContainingKtFile()
        val project = file.project
        val outermostParent = KtPsiUtil.getOutermostParent(element, file, false)

        val ktFilesToAddImports = SmartList<KtFile>()
        val javaCallsToFix = SmartList<PsiMethodCallExpression>()
        for (ref in ReferencesSearch.search(element)) {
            when (ref) {
                is KtReference -> {
                    val refFile = ref.element.getContainingKtFile()
                    if (refFile != file) {
                        ktFilesToAddImports.add(refFile)
                    }
                }
                is PsiReferenceExpression -> javaCallsToFix.addIfNotNull(ref.parent as? PsiMethodCallExpression)
            }
        }

        val typeParameterList = newTypeParameterList(element)

        val psiFactory = KtPsiFactory(element)

        val extension = file.addAfter(element, outermostParent) as KtCallableDeclaration
        file.addAfter(psiFactory.createNewLine(), outermostParent)
        file.addAfter(psiFactory.createNewLine(), outermostParent)
        element.delete()

        extension.setReceiverType(containingClass.defaultType)

        if (typeParameterList != null) {
            if (extension.typeParameterList != null) {
                extension.typeParameterList!!.replace(typeParameterList)
            }
            else {
                extension.addBefore(typeParameterList, extension.receiverTypeReference)
                extension.addBefore(psiFactory.createWhiteSpace(), extension.receiverTypeReference)
            }
        }

        extension.modifierList?.getModifier(KtTokens.PROTECTED_KEYWORD)?.delete()
        extension.modifierList?.getModifier(KtTokens.ABSTRACT_KEYWORD)?.delete()
        extension.modifierList?.getModifier(KtTokens.OPEN_KEYWORD)?.delete()
        extension.modifierList?.getModifier(KtTokens.FINAL_KEYWORD)?.delete()

        var bodyToSelect: KtExpression? = null

        fun selectBody(declaration: KtDeclarationWithBody) {
            if (bodyToSelect == null) {
                val body = declaration.bodyExpression
                bodyToSelect = (body as? KtBlockExpression)?.statements?.single() ?: body
            }
        }

        val bodyText = getFunctionBodyTextFromTemplate(
                project,
                if (extension is KtFunction) TemplateKind.FUNCTION else TemplateKind.PROPERTY_INITIALIZER,
                extension.name,
                extension.getReturnTypeReference()?.text ?: "Unit",
                extension.containingClassOrObject?.fqName
        )

        when (extension) {
            is KtFunction -> {
                if (!extension.hasBody()) {
                    //TODO: methods in PSI for setBody
                    extension.add(psiFactory.createBlock(bodyText))
                    selectBody(extension)
                }
            }

            is KtProperty -> {
                val templateProperty = psiFactory.createDeclaration<KtProperty>("var v: Any\nget()=$bodyText\nset(value){\n$bodyText\n}")
                val templateGetter = templateProperty.getter!!
                val templateSetter = templateProperty.setter!!

                var getter = extension.getter
                if (getter == null) {
                    getter = extension.addAfter(templateGetter, extension.typeReference) as KtPropertyAccessor
                    extension.addBefore(psiFactory.createNewLine(), getter)
                    selectBody(getter)
                }
                else if (!getter.hasBody()) {
                    getter = getter.replace(templateGetter) as KtPropertyAccessor
                    selectBody(getter)
                }

                if (extension.isVar) {
                    var setter = extension.setter
                    if (setter == null) {
                        setter = extension.addAfter(templateSetter, getter) as KtPropertyAccessor
                        extension.addBefore(psiFactory.createNewLine(), setter)
                        selectBody(setter)
                    }
                    else if (!setter.hasBody()) {
                        setter = setter.replace(templateSetter) as KtPropertyAccessor
                        selectBody(setter)
                    }
                }
            }
        }

        if (ktFilesToAddImports.isNotEmpty()) {
            val newDescriptor = extension.resolveToDescriptor()
            val importInsertHelper = ImportInsertHelper.getInstance(project)
            for (ktFileToAddImport in ktFilesToAddImports) {
                importInsertHelper.importDescriptor(ktFileToAddImport, newDescriptor)
            }
        }

        if (javaCallsToFix.isNotEmpty()) {
            val lightMethod = extension.toLightMethods().first()
            for (javaCallToFix in javaCallsToFix) {
                javaCallToFix.methodExpression.qualifierExpression?.let {
                    val argumentList = javaCallToFix.argumentList
                    argumentList.addBefore(it, argumentList.expressions.firstOrNull())
                }

                val newRef = javaCallToFix.methodExpression.bindToElement(lightMethod)
                JavaCodeStyleManager.getInstance(project).shortenClassReferences(newRef)
            }
        }

        editor?.apply {
            unblockDocument()

            if (bodyToSelect != null) {
                val range = bodyToSelect!!.textRange
                moveCaret(range.startOffset, ScrollType.CENTER)
                selectionModel.setSelection(range.startOffset, range.endOffset)
            }
            else {
                moveCaret(extension.textOffset, ScrollType.CENTER)
            }
        }
    }

    private fun newTypeParameterList(member: KtCallableDeclaration): KtTypeParameterList? {
        val classElement = member.parent.parent as KtClass
        val classParams = classElement.typeParameters
        if (classParams.isEmpty()) return null
        val allTypeParameters = classParams + member.typeParameters
        val text = allTypeParameters.map { it.text }.joinToString(",", "<", ">")
        return KtPsiFactory(member).createDeclaration<KtFunction>("fun $text foo()").typeParameterList
    }
}
