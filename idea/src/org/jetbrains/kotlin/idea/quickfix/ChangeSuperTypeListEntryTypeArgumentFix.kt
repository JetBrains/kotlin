/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

class ChangeSuperTypeListEntryTypeArgumentFix(
    element: KtSuperTypeListEntry,
    private val type: String,
    private val typeArgumentIndex: Int
) : KotlinQuickFixAction<KtSuperTypeListEntry>(element) {

    override fun getText() = "Change type argument to $type"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val superTypeListEntry = element ?: return

        val typeArgumentList = superTypeListEntry.typeAsUserType?.typeArgumentList?.arguments?.mapIndexed { index, typeProjection ->
            if (index == typeArgumentIndex) type else typeProjection.text
        }?.joinToString(prefix = "<", postfix = ">", separator = ", ") { it } ?: return

        val psiFactory = KtPsiFactory(superTypeListEntry)
        val newElement = when (superTypeListEntry) {
            is KtSuperTypeEntry -> {
                val classReference = superTypeListEntry.typeAsUserType?.referenceExpression?.text ?: return
                psiFactory.createSuperTypeEntry("$classReference$typeArgumentList")
            }
            is KtSuperTypeCallEntry -> {
                val classReference = superTypeListEntry.calleeExpression.constructorReferenceExpression?.text ?: return
                val valueArgumentList = superTypeListEntry.valueArgumentList?.text ?: return
                psiFactory.createSuperTypeCallEntry("$classReference$typeArgumentList$valueArgumentList")
            }
            else -> return
        }

        superTypeListEntry.replace(newElement)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val (casted, declaration) = when (diagnostic.factory) {
                Errors.RETURN_TYPE_MISMATCH_ON_OVERRIDE -> {
                    val casted = Errors.RETURN_TYPE_MISMATCH_ON_OVERRIDE.cast(diagnostic)
                    casted to casted.b.declaration
                }
                Errors.PROPERTY_TYPE_MISMATCH_ON_OVERRIDE -> {
                    val casted = Errors.PROPERTY_TYPE_MISMATCH_ON_OVERRIDE.cast(diagnostic)
                    casted to casted.b
                }
                else -> null
            } ?: return null

            val type = casted.a.returnType?.toString() ?: return null
            val superClassDescriptor = declaration.containingDeclaration as? ClassDescriptor ?: return null
            val superDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(declaration) as? KtNamedDeclaration ?: return null
            val superTypeReference = superDeclaration.getReturnTypeReference()?.text ?: return null
            val typeParameterIndex = superClassDescriptor.declaredTypeParameters.map { it.name.asString() }.indexOf(superTypeReference)
            if (typeParameterIndex < 0) return null

            val containingClass = casted.psiElement.containingClass() ?: return null
            val superTypeListEntry = containingClass.superTypeListEntries.find {
                when (it) {
                    is KtSuperTypeEntry -> {
                        (it.typeAsUserType?.referenceExpression?.mainReference?.resolve() as? KtClass)?.descriptor == superClassDescriptor
                    }
                    is KtSuperTypeCallEntry -> {
                        it.calleeExpression.resolveToCall()?.resultingDescriptor?.returnType?.constructor?.declarationDescriptor == superClassDescriptor
                    }
                    else -> false
                }
            } ?: return null

            return ChangeSuperTypeListEntryTypeArgumentFix(superTypeListEntry, type, typeParameterIndex)
        }
    }
}