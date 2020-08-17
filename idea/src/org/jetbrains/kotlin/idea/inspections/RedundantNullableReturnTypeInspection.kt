/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.isOverridable
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.resolve.getDataFlowValueFactory
import org.jetbrains.kotlin.idea.util.textRangeIn
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable

class RedundantNullableReturnTypeInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : KtVisitorVoid() {
        override fun visitNamedFunction(function: KtNamedFunction) {
            check(function)
        }

        override fun visitProperty(property: KtProperty) {
            if (property.isVar) return
            check(property)
        }

        private fun check(declaration: KtCallableDeclaration) {
            val typeReference = declaration.typeReference ?: return
            val typeElement = typeReference.typeElement as? KtNullableType ?: return
            if (typeElement.innerType == null) return
            val questionMark = typeElement.questionMarkNode as? LeafPsiElement ?: return

            if (declaration.isOverridable()) return

            val body = when (declaration) {
                is KtNamedFunction -> declaration.bodyExpression
                is KtProperty -> declaration.initializer ?: declaration.accessors.singleOrNull { it.isGetter }?.bodyExpression
                else -> null
            } ?: return
            val actualReturnTypes = body.actualReturnTypes()
            if (actualReturnTypes.isEmpty() || actualReturnTypes.any { it.isNullable() }) return

            val declarationName = declaration.nameAsSafeName.asString()
            val description = if (declaration is KtProperty) {
                KotlinBundle.message("0.is.always.non.null.type", declarationName)
            } else {
                KotlinBundle.message("0.always.returns.non.null.type", declarationName)
            }
            holder.registerProblem(
                typeReference,
                questionMark.textRangeIn(typeReference),
                description,
                MakeNotNullableFix()
            )
        }
    }

    private fun KtExpression.actualReturnTypes(): List<KotlinType> {
        val context = analyze()
        val dataFlowValueFactory = getResolutionFacade().getDataFlowValueFactory()
        val moduleDescriptor = findModuleDescriptor()
        val languageVersionSettings = languageVersionSettings
        return if (this is KtBlockExpression) {
            collectDescendantsOfType<KtReturnExpression>().flatMap {
                it.returnedExpression.types(context, dataFlowValueFactory, moduleDescriptor, languageVersionSettings)
            }
        } else {
            types(context, dataFlowValueFactory, moduleDescriptor, languageVersionSettings)
        }
    }

    private fun KtExpression?.types(
        context: BindingContext,
        dataFlowValueFactory: DataFlowValueFactory,
        moduleDescriptor: ModuleDescriptor,
        languageVersionSettings: LanguageVersionSettings
    ): List<KotlinType> {
        if (this == null) return emptyList()
        val type = context.getType(this) ?: return emptyList()
        val dataFlowInfo = context[BindingContext.EXPRESSION_TYPE_INFO, this]?.dataFlowInfo ?: return emptyList()
        val dataFlowValue = dataFlowValueFactory.createDataFlowValue(this, type, context, moduleDescriptor)
        val stableTypes = dataFlowInfo.getStableTypes(dataFlowValue, languageVersionSettings)
        return if (stableTypes.isNotEmpty()) stableTypes.toList() else listOf(type)
    }

    private class MakeNotNullableFix : LocalQuickFix {
        override fun getName() = KotlinBundle.message("make.not.nullable")

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val typeReference = descriptor.psiElement as? KtTypeReference ?: return
            val typeElement = typeReference.typeElement as? KtNullableType ?: return
            val innerType = typeElement.innerType ?: return
            typeElement.replace(innerType)
        }
    }
}
