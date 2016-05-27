/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageFeatureSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.callableReferences.createReflectionTypeForResolvedCallableReference
import org.jetbrains.kotlin.resolve.callableReferences.resolvePossiblyAmbiguousCallableReference
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeImpl
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.createTypeInfo
import javax.inject.Inject

sealed class DoubleColonLHS(val type: KotlinType) {
    class Expression(val typeInfo: KotlinTypeInfo) : DoubleColonLHS(typeInfo.type!!)

    class Type(type: KotlinType, val possiblyBareType: PossiblyBareType) : DoubleColonLHS(type)
}

// Returns true if this expression has the form "A<B>" which means it's a type on the LHS of a double colon expression
internal val KtCallExpression.isWithoutValueArguments: Boolean
    get() = valueArgumentList == null && lambdaArguments.isEmpty()

class DoubleColonExpressionResolver(
        val callResolver: CallResolver,
        val qualifiedExpressionResolver: QualifiedExpressionResolver,
        val dataFlowAnalyzer: DataFlowAnalyzer,
        val reflectionTypes: ReflectionTypes,
        val typeResolver: TypeResolver,
        val languageFeatureSettings: LanguageFeatureSettings
) {
    private lateinit var expressionTypingServices: ExpressionTypingServices

    // component dependency cycle
    @Inject
    fun setExpressionTypingServices(expressionTypingServices: ExpressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices
    }

    fun visitClassLiteralExpression(expression: KtClassLiteralExpression, c: ExpressionTypingContext): KotlinTypeInfo {
        if (expression.isEmptyLHS) {
            // "::class" will maybe mean "this::class", a class of "this" instance
            c.trace.report(UNSUPPORTED.on(expression, "Class literals with empty left hand side are not yet supported"))
        }
        else {
            val result = resolveDoubleColonLHS(expression.receiverExpression!!, expression, c)
            val type = result?.type
            if (type != null && !type.isError) {
                checkClassLiteral(c, expression, result!!)
                return dataFlowAnalyzer.createCheckedTypeInfo(reflectionTypes.getKClassType(Annotations.EMPTY, type), c, expression)
            }
        }

        return createTypeInfo(ErrorUtils.createErrorType("Unresolved class"), c)
    }

    private fun checkClassLiteral(c: ExpressionTypingContext, expression: KtClassLiteralExpression, result: DoubleColonLHS) {
        if (result !is DoubleColonLHS.Type) return

        val type = result.type
        val reportError: Boolean
        if (result.possiblyBareType.isBare) {
            val descriptor = type.constructor.declarationDescriptor
            if (descriptor is ClassDescriptor && KotlinBuiltIns.isNonPrimitiveArray(descriptor)) {
                c.trace.report(ARRAY_CLASS_LITERAL_REQUIRES_ARGUMENT.on(expression))
            }
            reportError = false
        }
        else {
            reportError = !isAllowedInClassLiteral(type)
        }

        if (type.isMarkedNullable || reportError) {
            c.trace.report(CLASS_LITERAL_LHS_NOT_A_CLASS.on(expression))
        }
    }

    // Returns true if the expression is not a call expression without value arguments (such as "A<B>") or a qualified expression
    // which contains such call expression as one of its parts.
    // In this case it's pointless to attempt to type check an expression on the LHS in "A<B>::class", since "A<B>" certainly means a type.
    private fun KtExpression.canBeConsideredProperExpression(): Boolean {
        return when (this) {
            is KtCallExpression ->
                !isWithoutValueArguments
            is KtDotQualifiedExpression ->
                receiverExpression.canBeConsideredProperExpression() &&
                selectorExpression?.let { it.canBeConsideredProperExpression() } ?: false
            else -> true
        }
    }

    private fun shouldTryResolveLHSAsExpression(expression: KtDoubleColonExpression): Boolean {
        // TODO: improve diagnostic when bound callable references are disabled
        if (!languageFeatureSettings.supportsFeature(LanguageFeature.BoundCallableReferences)) return false

        val lhs = expression.receiverExpression ?: return false
        return lhs.canBeConsideredProperExpression() && !expression.hasQuestionMarks /* TODO: test this */
    }

    private fun resolveDoubleColonLHS(
            expression: KtExpression, doubleColonExpression: KtDoubleColonExpression, c: ExpressionTypingContext
    ): DoubleColonLHS? {
        // First, try resolving the LHS as expression, if possible

        if (shouldTryResolveLHSAsExpression(doubleColonExpression)) {
            val traceForExpr = TemporaryTraceAndCache.create(c, "resolve '::' LHS as expression", expression)
            val contextForExpr = c.replaceTraceAndCache(traceForExpr)
            val typeInfo = expressionTypingServices.getTypeInfo(expression, contextForExpr)
            val type = typeInfo.type
            // TODO (!!!): it's wrong to only check type, should check that there's a companion qualifier
            if (type != null && !DescriptorUtils.isCompanionObject(type.constructor.declarationDescriptor)) {
                traceForExpr.commit()
                return DoubleColonLHS.Expression(typeInfo).apply {
                    c.trace.record(BindingContext.DOUBLE_COLON_LHS, expression, this)
                }
            }
        }

        // Then, try resolving it as type

        val qualifierResolutionResult =
                qualifiedExpressionResolver.resolveDescriptorForDoubleColonLHS(expression, c.scope, c.trace, c.isDebuggerContext)

        val typeResolutionContext = TypeResolutionContext(
                c.scope, c.trace, /* checkBounds = */ true, /* allowBareTypes = */ true,
                /* isDebuggerContext = */ expression.suppressDiagnosticsInDebugMode() /* TODO: test this */
        )

        val classifier = qualifierResolutionResult.classifierDescriptor
        if (classifier == null) {
            typeResolver.resolveTypeProjections(
                    typeResolutionContext, ErrorUtils.createErrorType("No type").constructor, qualifierResolutionResult.allProjections
            )
            return null
        }

        val possiblyBareType = typeResolver.resolveTypeForClassifier(
                typeResolutionContext, classifier, qualifierResolutionResult, expression, Annotations.EMPTY
        )

        val type = if (possiblyBareType.isBare) {
            val descriptor = possiblyBareType.bareTypeConstructor.declarationDescriptor as? ClassDescriptor
                             ?: error("Only classes can produce bare types: $possiblyBareType")

            if (doubleColonExpression is KtCallableReferenceExpression) {
                c.trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(expression, descriptor.typeConstructor.parameters.size, descriptor))
            }

            KotlinTypeImpl.create(
                    Annotations.EMPTY, descriptor, possiblyBareType.isNullable || doubleColonExpression.hasQuestionMarks,
                    descriptor.typeConstructor.parameters.map(TypeUtils::makeStarProjection)
            )
        }
        else {
            TypeUtils.makeNullableAsSpecified(possiblyBareType.actualType, doubleColonExpression.hasQuestionMarks)
        }

        return DoubleColonLHS.Type(type, possiblyBareType).apply {
            c.trace.record(BindingContext.DOUBLE_COLON_LHS, expression, this)
        }
    }

    private fun isAllowedInClassLiteral(type: KotlinType): Boolean {
        val typeConstructor = type.constructor
        val descriptor = typeConstructor.declarationDescriptor

        when (descriptor) {
            is ClassDescriptor -> {
                if (KotlinBuiltIns.isNonPrimitiveArray(descriptor)) {
                    return type.arguments.none { typeArgument ->
                        typeArgument.isStarProjection || !isAllowedInClassLiteral(typeArgument.type)
                    }
                }

                return type.arguments.isEmpty()
            }
            is TypeParameterDescriptor -> return descriptor.isReified
            else -> return false
        }
    }

    fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression, c: ExpressionTypingContext): KotlinTypeInfo {
        val callableReference = expression.callableReference
        if (callableReference.getReferencedName().isEmpty()) {
            expression.receiverExpression?.let { resolveDoubleColonLHS(it, expression, c) }
            c.trace.report(UNRESOLVED_REFERENCE.on(callableReference, callableReference))
            val errorType = ErrorUtils.createErrorType("Empty callable reference")
            return dataFlowAnalyzer.createCheckedTypeInfo(errorType, c, expression)
        }

        val (lhs, resolutionResults) = resolveCallableReference(expression, c, ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS)
        val result = getCallableReferenceType(expression, lhs, resolutionResults, c)
        return dataFlowAnalyzer.createCheckedTypeInfo(result, c, expression)
    }

    private fun getCallableReferenceType(
            expression: KtCallableReferenceExpression,
            lhs: DoubleColonLHS?,
            resolutionResults: OverloadResolutionResults<*>?,
            context: ExpressionTypingContext
    ): KotlinType? {
        val reference = expression.callableReference

        val descriptor =
                if (resolutionResults != null && !resolutionResults.isNothing) {
                    OverloadResolutionResultsUtil.getResultingCall(resolutionResults, context.contextDependency)?.let { call ->
                        call.resultingDescriptor
                    } ?: return null
                }
                else {
                    context.trace.report(UNRESOLVED_REFERENCE.on(reference, reference))
                    return null
                }

        if (expression.isEmptyLHS &&
            (descriptor.dispatchReceiverParameter != null || descriptor.extensionReceiverParameter != null)) {
            context.trace.report(CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS.on(reference))
        }

        val containingDeclaration = descriptor.containingDeclaration
        if (DescriptorUtils.isObject(containingDeclaration)) {
            context.trace.report(CALLABLE_REFERENCE_TO_OBJECT_MEMBER.on(reference))
        }
        if (descriptor is ConstructorDescriptor && DescriptorUtils.isAnnotationClass(containingDeclaration)) {
            context.trace.report(CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR.on(reference))
        }

        val ignoreReceiver = lhs is DoubleColonLHS.Expression || expression.isEmptyLHS
        return createReflectionTypeForResolvedCallableReference(
                expression, lhs?.type, ignoreReceiver, descriptor, context, reflectionTypes
        )
    }

    fun resolveCallableReference(
            expression: KtCallableReferenceExpression,
            context: ExpressionTypingContext,
            resolveArgumentsMode: ResolveArgumentsMode
    ): Pair<DoubleColonLHS?, OverloadResolutionResults<*>?> {
        val lhsResult = expression.receiverExpression?.let { resolveDoubleColonLHS(it, expression, context) }

        val resolutionResults = resolvePossiblyAmbiguousCallableReference(
                expression, lhsResult?.type, context, resolveArgumentsMode, callResolver
        )

        return lhsResult to resolutionResults
    }
}
