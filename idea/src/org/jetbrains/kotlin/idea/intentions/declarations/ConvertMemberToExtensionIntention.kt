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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.SmartList
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.highlighter.markers.headerImplementations
import org.jetbrains.kotlin.idea.highlighter.markers.isHeaderOrHeaderClassMember
import org.jetbrains.kotlin.idea.highlighter.markers.liftToHeader
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.idea.refactoring.withHeaderImplementations
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.utils.addIfNotNull

class ConvertMemberToExtensionIntention : SelfTargetingRangeIntention<KtCallableDeclaration>(KtCallableDeclaration::class.java, "Convert member to extension"), LowPriorityAction {
    private fun isApplicable(element: KtCallableDeclaration): Boolean {
        val classBody = element.parent as? KtClassBody ?: return false
        if (classBody.parent !is KtClass) return false
        if (element.receiverTypeReference != null) return false
        if (element.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return false
        when (element) {
            is KtProperty -> if (element.hasInitializer()) return false
            is KtSecondaryConstructor -> return false
        }
        return true
    }

    override fun applicabilityRange(element: KtCallableDeclaration): TextRange? {
        if (!element.withHeaderImplementations().all { it is KtCallableDeclaration && isApplicable(it) }) return null
        return (element.nameIdentifier ?: return null).textRange
    }

    override fun startInWriteAction() = false

    //TODO: local class

    override fun applyTo(element: KtCallableDeclaration, editor: Editor?) {
        var allowHeader = true

        element.liftToHeader()?.headerImplementations()?.let {
            if (it.isEmpty()) {
                allowHeader = askIfHeaderIsAllowed(element.containingKtFile)
            }
        }

        runWriteAction {
            val (extension, bodyToSelect) = createExtensionCallableAndPrepareBodyToSelect(element, allowHeader)

            editor?.apply {
                unblockDocument()

                if (bodyToSelect != null) {
                    val range = bodyToSelect.textRange
                    moveCaret(range.startOffset, ScrollType.CENTER)

                    val parent = bodyToSelect.parent
                    val lastSibling =
                            if (parent is KtBlockExpression)
                                parent.rBrace?.siblings(forward = false, withItself = false)?.first { it !is PsiWhiteSpace }
                            else
                                bodyToSelect.siblings(forward = true, withItself = false).lastOrNull()
                    val endOffset = lastSibling?.endOffset ?: range.endOffset
                    selectionModel.setSelection(range.startOffset, endOffset)
                }
                else {
                    moveCaret(extension.textOffset, ScrollType.CENTER)
                }
            }
        }
    }

    private fun processSingleDeclaration(
            element: KtCallableDeclaration,
            allowHeader: Boolean
    ): Pair<KtCallableDeclaration, KtExpression?> {
        val descriptor = element.resolveToDescriptor()
        val containingClass = descriptor.containingDeclaration as ClassDescriptor

        val isEffectiveHeader = allowHeader && element.isHeaderOrHeaderClassMember()

        val file = element.containingKtFile
        val project = file.project
        val outermostParent = KtPsiUtil.getOutermostParent(element, file, false)

        val ktFilesToAddImports = SmartList<KtFile>()
        val javaCallsToFix = SmartList<PsiMethodCallExpression>()
        for (ref in ReferencesSearch.search(element)) {
            when (ref) {
                is KtReference -> {
                    val refFile = ref.element.containingKtFile
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

        if (isEffectiveHeader && !extension.hasModifier(KtTokens.HEADER_KEYWORD))
        extension.addModifier(KtTokens.HEADER_KEYWORD)

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
                if (!extension.hasBody() && !isEffectiveHeader) {
                    //TODO: methods in PSI for setBody
                    extension.add(psiFactory.createBlock(bodyText))
                    selectBody(extension)
                }
            }

            is KtProperty -> {
                val templateProperty = psiFactory.createDeclaration<KtProperty>("var v: Any\nget()=$bodyText\nset(value){\n$bodyText\n}")

                if (!isEffectiveHeader) {
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

        return extension to bodyToSelect
    }

    private fun askIfHeaderIsAllowed(file: KtFile): Boolean {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return file.allChildren.any { it is PsiComment && it.text.trim() == "// ALLOW_HEADER_WITHOUT_IMPLS" }
        }

        return Messages.showYesNoDialog(
                "Do you want to make new extension a header declaration?",
                text,
                Messages.getQuestionIcon()
        ) == Messages.YES
    }

    private fun createExtensionCallableAndPrepareBodyToSelect(
            element: KtCallableDeclaration,
            allowHeader: Boolean = true
    ): Pair<KtCallableDeclaration, KtExpression?> {
        val headerDeclaration = element.liftToHeader() as? KtCallableDeclaration
        if (headerDeclaration != null) {
            element.withHeaderImplementations().filterIsInstance<KtCallableDeclaration>().forEach {
                if (it != element) {
                    processSingleDeclaration(it, allowHeader)
                }
            }
        }

        return processSingleDeclaration(element, allowHeader)
    }

    private fun newTypeParameterList(member: KtCallableDeclaration): KtTypeParameterList? {
        val classElement = member.parent.parent as KtClass
        val classParams = classElement.typeParameters
        if (classParams.isEmpty()) return null
        val allTypeParameters = classParams + member.typeParameters
        val text = allTypeParameters.joinToString(",", "<", ">") { it.text }
        return KtPsiFactory(member).createDeclaration<KtFunction>("fun $text foo()").typeParameterList
    }

    companion object {
        fun convert(element: KtCallableDeclaration): KtCallableDeclaration {
            return ConvertMemberToExtensionIntention().createExtensionCallableAndPrepareBodyToSelect(element).first
        }
    }
}
