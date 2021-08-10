/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.BuilderInferenceSession
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeIntersector
import org.jetbrains.kotlin.types.checkEnumsForCompatibility
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.expressions.SenselessComparisonChecker.checkSenselessComparisonWithNull
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.util.OperatorNameConventions

object EqualityCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val callExpression = resolvedCall.call.callElement as? KtBinaryExpression ?: return
        val operationType = callExpression.operationReference.getReferencedNameElementType()

        if (operationType in OperatorConventions.EQUALS_OPERATIONS) {
            checkEquality(resolvedCall, callExpression, context.resolutionContext)
        }
    }

    private fun checkEquality(resolvedCall: ResolvedCall<*>, expression: KtBinaryExpression, context: ResolutionContext<*>) {
        val returnType = resolvedCall.resultingDescriptor.returnType ?: return
        val builtIns = returnType.builtIns

        if (!builtIns.isBooleanOrSubtype(returnType)) {
            context.trace.report(
                Errors.RESULT_TYPE_MISMATCH.on(
                    expression.operationReference,
                    "'${OperatorNameConventions.EQUALS}'",
                    builtIns.booleanType,
                    returnType
                )
            )
            return
        }

        ensureNonemptyIntersectionOfOperandTypes(expression, context)
    }

    // We check identity equality separately because we don't have a call for it
    fun checkIdentityEquality(expression: KtBinaryExpression, context: ResolutionContext<*>) {
        ensureNonemptyIntersectionOfOperandTypes(expression, context)
        checkIdentityOnPrimitiveOrInlineClassTypes(expression, context)
    }

    private fun checkIdentityOnPrimitiveOrInlineClassTypes(expression: KtBinaryExpression, context: ResolutionContext<*>) {
        val left = expression.left ?: return
        val right = expression.right ?: return

        val leftType = context.trace.getType(left) ?: return
        val rightType = context.trace.getType(right) ?: return

        if (KotlinTypeChecker.DEFAULT.equalTypes(leftType, rightType)) {
            if (KotlinBuiltIns.isPrimitiveType(leftType)) {
                context.trace.report(Errors.DEPRECATED_IDENTITY_EQUALS.on(expression, leftType, rightType))
            }
        } else if (isIdentityComparedWithImplicitBoxing(leftType, rightType) || isIdentityComparedWithImplicitBoxing(rightType, leftType)) {
            context.trace.report(Errors.IMPLICIT_BOXING_IN_IDENTITY_EQUALS.on(expression, leftType, rightType))
        }
        if (leftType.isInlineClassType() || rightType.isInlineClassType()) {
            context.trace.report(Errors.FORBIDDEN_IDENTITY_EQUALS.on(expression, leftType, rightType))
        }
    }

    private fun isIdentityComparedWithImplicitBoxing(leftType: KotlinType, rightType: KotlinType) =
        KotlinBuiltIns.isPrimitiveType(leftType)
                && !KotlinBuiltIns.isPrimitiveType(rightType)
                && KotlinTypeChecker.DEFAULT.isSubtypeOf(leftType, rightType)

    private fun ensureNonemptyIntersectionOfOperandTypes(expression: KtBinaryExpression, context: ResolutionContext<*>) {
        val left = expression.left ?: return
        val right = expression.right ?: return

        val leftType = context.trace.getType(left) ?: return
        val rightType = context.trace.getType(right) ?: return

        if (TypeIntersector.isIntersectionEmpty(leftType, rightType)) {
            val isProperEqualityChecksEnabled =
                context.languageVersionSettings.supportsFeature(LanguageFeature.ProperEqualityChecksInBuilderInferenceCalls)
            val shouldReportWarnings = !isProperEqualityChecksEnabled
                    && context.inferenceSession is BuilderInferenceSession
                    && context.trace.get(BindingContext.MARKED_EQUALIY_CALL_PROPER_IN_BUILDER_INFERENCE, expression) != null
            val diagnostic = if (shouldReportWarnings) Errors.EQUALITY_NOT_APPLICABLE_WARNING else Errors.EQUALITY_NOT_APPLICABLE

            context.trace.report(diagnostic.on(expression, expression.operationReference, leftType, rightType))
        } else {
            checkEnumsForCompatibility(context, expression, leftType, rightType)
        }

        checkSenselessComparisonWithNull(
            expression, left, right, context, context.trace::getType, context.dataFlowInfo::getStableNullability
        )
    }
}