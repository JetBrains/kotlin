/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToParameterDescriptorIfAny
import org.jetbrains.kotlin.idea.intentions.RemoveEmptyPrimaryConstructorIntention
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RemoveUnusedFunctionParameterFix(parameter: KtParameter, private val checkUsages: Boolean = true) :
    KotlinQuickFixAction<KtParameter>(parameter) {
    override fun getFamilyName() = ChangeFunctionSignatureFix.FAMILY_NAME

    override fun getText() = element?.let { KotlinBundle.message("remove.parameter.0", it.name.toString()) } ?: ""

    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val parameter = element ?: return
        val parameterList = parameter.parent as? KtParameterList ?: return
        if (!checkUsages) {
            runWriteAction {
                parameterList.removeParameter(parameter)
            }
            return
        }

        val parameterDescriptor = parameter.resolveToParameterDescriptorIfAny(BodyResolveMode.FULL) ?: return
        val parameterSize = parameterList.parameters.size
        val typeParameters = typeParameters(parameter.typeReference)
        val primaryConstructor = parameterList.parent as? KtPrimaryConstructor

        ChangeFunctionSignatureFix.runRemoveParameter(parameterDescriptor, parameter)
        runRemoveUnusedTypeParameters(typeParameters)

        if (parameterSize > 1) {
            val nextParameter = parameterList.parameters.getOrNull(parameterDescriptor.index)
            if (nextParameter != null) {
                editor?.caretModel?.moveToOffset(nextParameter.startOffset)
            }
        }

        if (primaryConstructor != null) {
            val removeConstructorIntention = RemoveEmptyPrimaryConstructorIntention()
            if (removeConstructorIntention.isApplicableTo(primaryConstructor)) {
                editor?.caretModel?.moveToOffset(primaryConstructor.endOffset)
                runWriteAction {
                    removeConstructorIntention.applyTo(primaryConstructor, editor = null)
                }
            }
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtParameter>? {
            val parameter = Errors.UNUSED_PARAMETER.cast(diagnostic).psiElement
            val parameterOwner = parameter.parent.parent
            if (parameterOwner is KtFunctionLiteral
                || (parameterOwner is KtNamedFunction && parameterOwner.name == null)
                || parameterOwner is KtPropertyAccessor
            ) return null
            return RemoveUnusedFunctionParameterFix(parameter)
        }

        fun typeParameters(typeReference: KtTypeReference?): List<KtTypeParameter> {
            if (typeReference == null) return emptyList()
            val parameterParent = typeReference.getParentOfTypesAndPredicate(
                true,
                KtNamedFunction::class.java, KtProperty::class.java, KtClass::class.java,
            ) { true }
            return typeReference.typeElement
                ?.collectDescendantsOfType<KtNameReferenceExpression> { true }
                ?.mapNotNull {
                    val typeParameter = it.reference?.resolve() as? KtTypeParameter ?: return@mapNotNull null
                    val parent = typeParameter.getParentOfTypesAndPredicate(
                        true,
                        KtNamedFunction::class.java, KtProperty::class.java, KtClass::class.java,
                    ) { true }
                    if (parent == parameterParent) typeParameter else null
                } ?: emptyList()
        }

        fun runRemoveUnusedTypeParameters(typeParameters: List<KtTypeParameter>) {
            val unusedTypeParams = typeParameters.filter { typeParameter ->
                ReferencesSearch.search(typeParameter).none { (it as? KtSimpleNameReference)?.expression?.parent !is KtTypeConstraint }
            }
            if (unusedTypeParams.isEmpty()) return
            runWriteAction {
                unusedTypeParams.forEach { typeParameter ->
                    val typeParameterList = typeParameter.parent as? KtTypeParameterList ?: return@forEach
                    val typeConstraintList = typeParameterList.parent.getChildOfType<KtTypeConstraintList>()
                    if (typeConstraintList != null) {
                        val typeConstraint = typeConstraintList.constraints.find { it.subjectTypeParameterName?.text == typeParameter.text }
                        if (typeConstraint != null) EditCommaSeparatedListHelper.removeItem(typeConstraint)
                        if (typeConstraintList.constraints.size == 0) {
                            val prev = typeConstraintList.getPrevSiblingIgnoringWhitespaceAndComments()
                            if (prev?.node?.elementType == KtTokens.WHERE_KEYWORD) prev?.delete()
                        }
                    }
                    if (typeParameterList.parameters.size == 1)
                        typeParameterList.delete()
                    else
                        EditCommaSeparatedListHelper.removeItem(typeParameter)
                }
            }
        }
    }
}
