/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.synthetic.canBePropertyAccessor
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

class KotlinRedundantOverrideInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
        namedFunctionVisitor(fun(function) {
            val funKeyword = function.funKeyword ?: return
            val modifierList = function.modifierList ?: return
            if (!modifierList.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return
            if (MODIFIER_EXCLUDE_OVERRIDE.any { modifierList.hasModifier(it) }) return
            if (function.annotationEntries.isNotEmpty()) return
            if (function.containingClass()?.isData() == true) return

            val bodyExpression = function.bodyExpression ?: return
            val qualifiedExpression = when (bodyExpression) {
                is KtDotQualifiedExpression -> bodyExpression
                is KtBlockExpression -> {
                    when (val body = bodyExpression.statements.singleOrNull()) {
                        is KtReturnExpression -> body.returnedExpression
                        is KtDotQualifiedExpression -> body.takeIf { _ ->
                            function.typeReference.let { it == null || it.text == "Unit" }
                        }
                        else -> null
                    }

                }
                else -> null
            } as? KtDotQualifiedExpression ?: return

            val superExpression = qualifiedExpression.receiverExpression as? KtSuperExpression ?: return
            if (superExpression.superTypeQualifier != null) return

            val superCallElement = qualifiedExpression.selectorExpression as? KtCallElement ?: return
            if (!isSameFunctionName(superCallElement, function)) return
            if (!isSameArguments(superCallElement, function)) return

            val context = function.analyze()
            val functionDescriptor = context[BindingContext.FUNCTION, function]
            val functionParams = functionDescriptor?.valueParameters.orEmpty()
            val superCallFunctionParams = superCallElement.resolveToCall()?.resultingDescriptor?.valueParameters.orEmpty()
            if (functionParams.size == superCallFunctionParams.size
                && functionParams.zip(superCallFunctionParams).any { it.first.type != it.second.type }
            ) return

            if (function.hasDerivedProperty(functionDescriptor, context)) return
            val overriddenDescriptors = functionDescriptor?.original?.overriddenDescriptors
            if (overriddenDescriptors?.any { it is JavaMethodDescriptor && it.visibility == JavaDescriptorVisibilities.PACKAGE_VISIBILITY } == true) return
            if (function.isAmbiguouslyDerived(overriddenDescriptors, context)) return

            val descriptor = holder.manager.createProblemDescriptor(
                function,
                TextRange(modifierList.startOffsetInParent, funKeyword.endOffset - function.startOffset),
                KotlinBundle.message("redundant.overriding.method"),
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                isOnTheFly,
                RedundantOverrideFix()
            )
            holder.registerProblem(descriptor)
        })

    private fun isSameArguments(superCallElement: KtCallElement, function: KtNamedFunction): Boolean {
        val arguments = superCallElement.valueArguments
        val parameters = function.valueParameters
        if (arguments.size != parameters.size) return false
        return arguments.zip(parameters).all { (argument, parameter) ->
            argument.getArgumentExpression()?.text == parameter.name
        }
    }

    private fun isSameFunctionName(superSelectorExpression: KtCallElement, function: KtNamedFunction): Boolean {
        val superCallMethodName = superSelectorExpression.getCallNameExpression()?.text ?: return false
        return function.name == superCallMethodName
    }

    private fun KtNamedFunction.hasDerivedProperty(functionDescriptor: FunctionDescriptor?, context: BindingContext): Boolean {
        if (functionDescriptor == null) return false
        val functionName = nameAsName ?: return false
        if (!canBePropertyAccessor(functionName.asString())) return false
        val functionType = functionDescriptor.returnType
        val isSetter = functionType?.isUnit() == true
        val valueParameters = valueParameters
        val singleValueParameter = valueParameters.singleOrNull()
        if (isSetter && singleValueParameter == null || !isSetter && valueParameters.isNotEmpty()) return false
        val propertyType =
            (if (isSetter) context[BindingContext.VALUE_PARAMETER, singleValueParameter]?.type else functionType)?.makeNotNullable()
        return SyntheticJavaPropertyDescriptor.propertyNamesByAccessorName(functionName).any {
            val propertyName = it.asString()
            containingClassOrObject?.declarations?.find { d ->
                d is KtProperty && d.name == propertyName && d.resolveToDescriptorIfAny()?.type?.makeNotNullable() == propertyType
            } != null
        }
    }

    private class RedundantOverrideFix : LocalQuickFix {
        override fun getName() = KotlinBundle.message("redundant.override.fix.text")
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            descriptor.psiElement.delete()
        }
    }

    companion object {
        private val MODIFIER_EXCLUDE_OVERRIDE = KtTokens.MODIFIER_KEYWORDS_ARRAY.asList() - KtTokens.OVERRIDE_KEYWORD
    }
}

private fun KtNamedFunction.isAmbiguouslyDerived(overriddenDescriptors: Collection<FunctionDescriptor>?, context: BindingContext): Boolean {
    if (overriddenDescriptors == null || overriddenDescriptors.size < 2) return false
    // Two+ functions
    // At least one default in interface or abstract in class, or just something from Java
    if (overriddenDescriptors.any { overriddenFunction ->
            overriddenFunction is JavaMethodDescriptor || when ((overriddenFunction.containingDeclaration as? ClassDescriptor)?.kind) {
                ClassKind.CLASS -> overriddenFunction.modality == Modality.ABSTRACT
                ClassKind.INTERFACE -> overriddenFunction.modality != Modality.ABSTRACT
                else -> false
            }

        }
    ) return true

    val delegatedSuperTypeEntries =
        containingClassOrObject?.superTypeListEntries?.filterIsInstance<KtDelegatedSuperTypeEntry>() ?: return false
    if (delegatedSuperTypeEntries.isEmpty()) return false
    val delegatedSuperDeclarations = delegatedSuperTypeEntries.mapNotNull { entry ->
        context[BindingContext.TYPE, entry.typeReference]?.constructor?.declarationDescriptor
    }
    return overriddenDescriptors.any {
        it.containingDeclaration in delegatedSuperDeclarations
    }
}

