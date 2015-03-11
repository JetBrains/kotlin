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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.resolve.AdditionalCheckerProvider
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.resolve.annotations.hasPlatformStaticAnnotation
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.resolve.annotations.hasInlineAnnotation
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.psi.JetTypeParameter
import org.jetbrains.kotlin.resolve.annotations.hasIntrinsicAnnotation
import org.jetbrains.kotlin.load.kotlin.nativeDeclarations.NativeFunChecker
import org.jetbrains.kotlin.psi.JetPropertyAccessor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.resolve.jvm.calls.checkers.NeedSyntheticChecker
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.load.java.lazy.types.isMarkedNullable
import org.jetbrains.kotlin.load.java.lazy.types.isMarkedNotNull
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.NullabilityInformationSource
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability
import org.jetbrains.kotlin.psi.JetPostfixExpression
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.expressions.SenselessComparisonChecker

public object KotlinJvmCheckerProvider : AdditionalCheckerProvider(
        additionalAnnotationCheckers = listOf(PlatformStaticAnnotationChecker(), LocalFunInlineChecker(), ReifiedTypeParameterAnnotationChecker(), NativeFunChecker()),
        additionalCallCheckers = listOf(NeedSyntheticChecker()),
        additionalTypeCheckers = listOf(JavaNullabilityWarningsChecker())
)

public class LocalFunInlineChecker : AnnotationChecker {

    override fun check(declaration: JetDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink) {
        if (descriptor.hasInlineAnnotation() &&
            declaration is JetNamedFunction &&
            descriptor is FunctionDescriptor &&
            descriptor.getVisibility() == Visibilities.LOCAL) {
            diagnosticHolder.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(declaration, declaration, descriptor))
        }
    }
}

public class PlatformStaticAnnotationChecker : AnnotationChecker {

    override fun check(declaration: JetDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink) {
        if (descriptor.hasPlatformStaticAnnotation()) {
            if (declaration is JetNamedFunction || declaration is JetProperty || declaration is JetPropertyAccessor) {
                checkDeclaration(declaration, descriptor, diagnosticHolder)
            }
            else {
                //TODO: there should be general mechanism
                diagnosticHolder.report(ErrorsJvm.PLATFORM_STATIC_ILLEGAL_USAGE.on(declaration, descriptor));
            }
        }
    }

    private fun checkDeclaration(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink
    ) {
        val container = descriptor.getContainingDeclaration()
        val insideObject = container != null && DescriptorUtils.isNonDefaultObject(container)
        val insideDefaultObjectInClass =
                container != null && DescriptorUtils.isDefaultObject(container) && DescriptorUtils.isClass(container.getContainingDeclaration())
                container != null && DescriptorUtils.isDefaultObject(container) && DescriptorUtils.isClass(container.getContainingDeclaration())

        if (!insideObject && !insideDefaultObjectInClass) {
            diagnosticHolder.report(ErrorsJvm.PLATFORM_STATIC_NOT_IN_OBJECT.on(declaration));
        }

        if (insideObject && descriptor is MemberDescriptor && descriptor.getModality().isOverridable()) {
            diagnosticHolder.report(ErrorsJvm.OPEN_CANNOT_BE_STATIC.on(declaration));
        }
    }
}

public class ReifiedTypeParameterAnnotationChecker : AnnotationChecker {

    override fun check(declaration: JetDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink) {
        if (descriptor.hasIntrinsicAnnotation()) return

        if (descriptor is CallableDescriptor && !descriptor.hasInlineAnnotation()) {
            checkTypeParameterDescriptorsAreNotReified(descriptor.getTypeParameters(), diagnosticHolder)
        }
        if (descriptor is ClassDescriptor) {
            checkTypeParameterDescriptorsAreNotReified(descriptor.getTypeConstructor().getParameters(), diagnosticHolder)
        }
    }

}

private fun checkTypeParameterDescriptorsAreNotReified(
        typeParameterDescriptors: List<TypeParameterDescriptor>,
        diagnosticHolder: DiagnosticSink
) {
    for (reifiedTypeParameterDescriptor in typeParameterDescriptors.filter { it.isReified() }) {
        val typeParameterDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(reifiedTypeParameterDescriptor)
        if (typeParameterDeclaration !is JetTypeParameter) throw AssertionError("JetTypeParameter expected")

        diagnosticHolder.report(
                Errors.REIFIED_TYPE_PARAMETER_NO_INLINE.on(
                        typeParameterDeclaration.getModifierList()!!.getModifier(JetTokens.REIFIED_KEYWORD)!!
                )
        )
    }
}

public class JavaNullabilityWarningsChecker : AdditionalTypeChecker {
    private fun JetType.mayBeNull(): NullabilityInformationSource? {
        if (!isError() && !isFlexible() && TypeUtils.isNullableType(this)) return NullabilityInformationSource.KOTLIN
        if (getAnnotations().isMarkedNullable()) return NullabilityInformationSource.JAVA
        return null
    }

    private fun JetType.mustNotBeNull(): NullabilityInformationSource? {
        if (!isError() && !isFlexible() && !TypeUtils.isNullableType(this)) return NullabilityInformationSource.KOTLIN
        if (!isMarkedNullable() && getAnnotations().isMarkedNotNull()) return NullabilityInformationSource.JAVA
        return null
    }

    private fun doCheckType(
            expressionType: JetType,
            expectedType: JetType,
            dataFlowValue: DataFlowValue,
            dataFlowInfo: DataFlowInfo,
            reportWarning: (expectedMustNotBeNull: NullabilityInformationSource, actualMayBeNull: NullabilityInformationSource) -> Unit
    ) {
        if (TypeUtils.noExpectedType(expectedType)) return

        val expectedMustNotBeNull = expectedType.mustNotBeNull()
        if (dataFlowInfo.getNullability(dataFlowValue) == Nullability.NOT_NULL) return

        val actualMayBeNull = expressionType.mayBeNull()

        if (expectedMustNotBeNull == NullabilityInformationSource.KOTLIN && actualMayBeNull == NullabilityInformationSource.KOTLIN) {
            // a type mismatch error will be reported elsewhere
            return;
        }

        if (expectedMustNotBeNull != null && actualMayBeNull != null) {
            reportWarning(expectedMustNotBeNull, actualMayBeNull)
        }
    }

    override fun checkType(expression: JetExpression, expressionType: JetType, c: ResolutionContext<*>) {
        doCheckType(
                expressionType,
                c.expectedType,
                DataFlowValueFactory.createDataFlowValue(expression, expressionType, c.trace.getBindingContext()),
                c.dataFlowInfo
        ) {
            expectedMustNotBeNull,
            actualMayBeNull ->
            c.trace.report(ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS.on(expression, expectedMustNotBeNull, actualMayBeNull))
        }

        when (expression) {
            is JetPostfixExpression ->
                    if (expression.getOperationToken() == JetTokens.EXCLEXCL) {
                        val baseExpression = expression.getBaseExpression()
                        val baseExpressionType = c.trace.get(BindingContext.EXPRESSION_TYPE, baseExpression) ?: return
                        doIfNotNull(
                                DataFlowValueFactory.createDataFlowValue(baseExpression, baseExpressionType, c.trace.getBindingContext()),
                                c
                        ) {
                            c.trace.report(Errors.UNNECESSARY_NOT_NULL_ASSERTION.on(expression.getOperationReference(), baseExpressionType))
                        }
                    }
            is JetBinaryExpression ->
                when (expression.getOperationToken()) {
                    JetTokens.ELVIS -> {
                        val baseExpression = expression.getLeft()
                        val baseExpressionType = c.trace.get(BindingContext.EXPRESSION_TYPE, baseExpression) ?: return
                        doIfNotNull(
                                DataFlowValueFactory.createDataFlowValue(baseExpression, baseExpressionType, c.trace.getBindingContext()),
                                c
                        ) {
                            c.trace.report(Errors.USELESS_ELVIS.on(expression.getOperationReference(), baseExpressionType))
                        }
                    }
                    JetTokens.EQEQ,
                    JetTokens.EXCLEQ,
                    JetTokens.EQEQEQ,
                    JetTokens.EXCLEQEQEQ -> {
                        if (expression.getLeft() != null && expression.getRight() != null) {
                            SenselessComparisonChecker.checkSenselessComparisonWithNull(
                                    expression, expression.getLeft()!!, expression.getRight()!!, c.trace,
                                    { c.trace.get(BindingContext.EXPRESSION_TYPE, it) },
                                    {
                                        value ->
                                        doIfNotNull(value, c) { Nullability.NOT_NULL } ?: Nullability.UNKNOWN
                                    }
                            )
                        }
                    }
                }
        }
    }

    private fun <T: Any> doIfNotNull(dataFlowValue: DataFlowValue, c: ResolutionContext<*>, body: () -> T): T? {
        if (c.dataFlowInfo.getNullability(dataFlowValue).canBeNull()
            && dataFlowValue.getType().mustNotBeNull() == NullabilityInformationSource.JAVA) {
            return body()
        }
        return null
    }

    override fun checkReceiver(
            receiverParameter: ReceiverParameterDescriptor,
            receiverArgument: ReceiverValue,
            safeAccess: Boolean,
            c: CallResolutionContext<*>
    ) {
        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiverArgument, c.trace.getBindingContext())
        if (!safeAccess) {
            doCheckType(
                    receiverArgument.getType(),
                    receiverParameter.getType(),
                    dataFlowValue,
                    c.dataFlowInfo
            ) {
                expectedMustNotBeNull,
                actualMayBeNull ->
                val reportOn =
                        if (receiverArgument is ExpressionReceiver)
                            receiverArgument.getExpression()
                        else
                            c.call.getCalleeExpression() ?: c.call.getCallElement()

                c.trace.report(ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS.on(
                        reportOn, expectedMustNotBeNull, actualMayBeNull
                ))

            }
        }
        else {
            // TODO: Compiler bug
            doIfNotNull(dataFlowValue, c as ResolutionContext<*>) {
                c.trace.report(Errors.UNNECESSARY_SAFE_CALL.on(c.call.getCallOperationNode().getPsi(), receiverArgument.getType()))
            }
        }
    }
}