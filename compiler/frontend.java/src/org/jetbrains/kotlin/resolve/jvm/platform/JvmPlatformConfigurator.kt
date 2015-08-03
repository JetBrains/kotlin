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

package org.jetbrains.kotlin.resolve.jvm.platform

import org.jetbrains.kotlin.cfg.WhenChecker
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.jvm.RuntimeAssertionsTypeChecker
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.load.java.lazy.types.isMarkedNotNull
import org.jetbrains.kotlin.load.java.lazy.types.isMarkedNullable
import org.jetbrains.kotlin.load.kotlin.JavaAnnotationCallChecker
import org.jetbrains.kotlin.load.kotlin.JavaAnnotationMethodCallChecker
import org.jetbrains.kotlin.load.kotlin.nativeDeclarations.NativeFunChecker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.annotations.findPublicFieldAnnotation
import org.jetbrains.kotlin.resolve.annotations.hasInlineAnnotation
import org.jetbrains.kotlin.resolve.annotations.hasIntrinsicAnnotation
import org.jetbrains.kotlin.resolve.annotations.hasPlatformStaticAnnotation
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability
import org.jetbrains.kotlin.resolve.descriptorUtil.getAnnotationRetention
import org.jetbrains.kotlin.resolve.descriptorUtil.isRepeatableAnnotation
import org.jetbrains.kotlin.resolve.jvm.calls.checkers.NeedSyntheticChecker
import org.jetbrains.kotlin.resolve.jvm.calls.checkers.ReflectionAPICallChecker
import org.jetbrains.kotlin.resolve.jvm.calls.checkers.TraitDefaultMethodCallChecker
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.NullabilityInformationSource
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.SenselessComparisonChecker

public object JvmPlatformConfigurator : PlatformConfigurator(
        DynamicTypesSettings(),
        additionalDeclarationCheckers = listOf(
                PlatformStaticAnnotationChecker(),
                LocalFunInlineChecker(),
                ReifiedTypeParameterAnnotationChecker(),
                NativeFunChecker(),
                OverloadsAnnotationChecker(),
                PublicFieldAnnotationChecker()
        ),

        additionalCallCheckers = listOf(
                NeedSyntheticChecker(),
                JavaAnnotationCallChecker(),
                JavaAnnotationMethodCallChecker(),
                TraitDefaultMethodCallChecker()
        ),

        additionalTypeCheckers = listOf(
                JavaNullabilityWarningsChecker(),
                RuntimeAssertionsTypeChecker
        ),

        additionalSymbolUsageValidators = listOf(),

        additionalAnnotationCheckers = listOf(RepeatableAnnotationChecker)
) {

    override fun configure(container: StorageComponentContainer) {
        super.configure(container)

        container.useImpl<ReflectionAPICallChecker>()
    }
}

public object RepeatableAnnotationChecker: AdditionalAnnotationChecker {
    override fun checkEntries(entries: List<JetAnnotationEntry>, actualTargets: List<KotlinTarget>, trace: BindingTrace) {
        val entryTypes: MutableSet<JetType> = hashSetOf()
        for (entry in entries) {
            val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: continue
            val classDescriptor = TypeUtils.getClassDescriptor(descriptor.type) ?: continue
            if (!entryTypes.add(descriptor.type)
                && classDescriptor.isRepeatableAnnotation()
                && classDescriptor.getAnnotationRetention() != KotlinRetention.SOURCE) {
                trace.report(ErrorsJvm.NON_SOURCE_REPEATED_ANNOTATION.on(entry));
            }
        }
    }
}

public class LocalFunInlineChecker : DeclarationChecker {

    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext) {
        if (descriptor.hasInlineAnnotation() &&
            declaration is JetNamedFunction &&
            descriptor is FunctionDescriptor &&
            descriptor.getVisibility() == Visibilities.LOCAL) {
            diagnosticHolder.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(declaration, declaration, descriptor))
        }
    }
}

public class PlatformStaticAnnotationChecker : DeclarationChecker {

    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (descriptor.hasPlatformStaticAnnotation()) {
            if (declaration is JetNamedFunction || declaration is JetProperty || declaration is JetPropertyAccessor) {
                checkDeclaration(declaration, descriptor, diagnosticHolder)
            }
        }
    }

    private fun checkDeclaration(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink
    ) {
        val container = descriptor.getContainingDeclaration()
        val insideObject = container != null && DescriptorUtils.isNonCompanionObject(container)
        val insideCompanionObjectInClass =
                container != null && DescriptorUtils.isCompanionObject(container) &&
                container.getContainingDeclaration().let { DescriptorUtils.isClass(it) || DescriptorUtils.isEnumClass(it) }

        if (!insideObject && !insideCompanionObjectInClass) {
            diagnosticHolder.report(ErrorsJvm.PLATFORM_STATIC_NOT_IN_OBJECT.on(declaration))
        }

        val checkDeclaration = when(declaration) {
            is JetPropertyAccessor -> declaration.getParent() as JetProperty
            else -> declaration
        }

        if (insideObject && checkDeclaration.getModifierList()?.hasModifier(JetTokens.OVERRIDE_KEYWORD) == true) {
            diagnosticHolder.report(ErrorsJvm.OVERRIDE_CANNOT_BE_STATIC.on(declaration))
        }
    }
}

public class OverloadsAnnotationChecker: DeclarationChecker {
    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (descriptor.getAnnotations().findAnnotation(FqName("kotlin.jvm.jvmOverloads")) != null) {
            checkDeclaration(declaration, descriptor, diagnosticHolder)
        }
    }

    private fun checkDeclaration(declaration: JetDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink) {
        if (descriptor !is CallableDescriptor) {
            return
        }
        if (descriptor is FunctionDescriptor && descriptor.getModality() == Modality.ABSTRACT) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_ABSTRACT.on(declaration))
        }
        else if ((!descriptor.getVisibility().isPublicAPI() && descriptor.getVisibility() != Visibilities.INTERNAL) ||
                 DescriptorUtils.isLocal(descriptor)) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_PRIVATE.on(declaration))
        }
        else if (descriptor.getValueParameters().none { it.declaresDefaultValue() }) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS.on(declaration))
        }
    }
}

public class PublicFieldAnnotationChecker: DeclarationChecker {
    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        val annotation = descriptor.findPublicFieldAnnotation() ?: return

        fun report() {
            val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: return
            diagnosticHolder.report(ErrorsJvm.INAPPLICABLE_PUBLIC_FIELD.on(annotationEntry))
        }

        if (descriptor is PropertyDescriptor
            && !bindingContext.get<PropertyDescriptor, Boolean>(BindingContext.BACKING_FIELD_REQUIRED, descriptor)!!) {
            report()
        }
    }
}

public class ReifiedTypeParameterAnnotationChecker : DeclarationChecker {

    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
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

        if (isFlexible() && TypeUtils.isNullableType(flexibility().lowerBound)) return NullabilityInformationSource.KOTLIN

        if (getAnnotations().isMarkedNullable()) return NullabilityInformationSource.JAVA
        return null
    }

    private fun JetType.mustNotBeNull(): NullabilityInformationSource? {
        if (!isError() && !isFlexible() && !TypeUtils.isNullableType(this)) return NullabilityInformationSource.KOTLIN

        if (isFlexible() && !TypeUtils.isNullableType(flexibility().upperBound)) return NullabilityInformationSource.KOTLIN

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
                DataFlowValueFactory.createDataFlowValue(expression, expressionType, c),
                c.dataFlowInfo
        ) {
            expectedMustNotBeNull,
            actualMayBeNull ->
            c.trace.report(ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS.on(expression, expectedMustNotBeNull, actualMayBeNull))
        }

        when (expression) {
            is JetWhenExpression ->
                    if (expression.getElseExpression() == null) {
                        // Check for conditionally-exhaustive when on platform enums, see KT-6399
                        val type = expression.getSubjectExpression()?.let { c.trace.getType(it) } ?: return
                        if (type.isFlexible() && TypeUtils.isNullableType(type.flexibility().upperBound) && !type.getAnnotations().isMarkedNotNull()) {
                            val enumClassDescriptor = WhenChecker.getClassDescriptorOfTypeIfEnum(type) ?: return

                            if (WhenChecker.isWhenOnEnumExhaustive(expression, c.trace, enumClassDescriptor)
                                && !WhenChecker.containsNullCase(expression, c.trace)) {

                                c.trace.report(ErrorsJvm.WHEN_ENUM_CAN_BE_NULL_IN_JAVA.on(expression.getSubjectExpression()))
                            }
                        }
                    }
            is JetPostfixExpression ->
                    if (expression.getOperationToken() == JetTokens.EXCLEXCL) {
                        val baseExpression = expression.getBaseExpression() ?: return
                        val baseExpressionType = c.trace.getType(baseExpression) ?: return
                        doIfNotNull(
                                DataFlowValueFactory.createDataFlowValue(baseExpression, baseExpressionType, c),
                                c
                        ) {
                            c.trace.report(Errors.UNNECESSARY_NOT_NULL_ASSERTION.on(expression.getOperationReference(), baseExpressionType))
                        }
                    }
            is JetBinaryExpression ->
                when (expression.getOperationToken()) {
                    JetTokens.ELVIS -> {
                        val baseExpression = expression.getLeft()
                        val baseExpressionType = baseExpression?.let{ c.trace.getType(it) } ?: return
                        doIfNotNull(
                                DataFlowValueFactory.createDataFlowValue(baseExpression!!, baseExpressionType, c),
                                c
                        ) {
                            c.trace.report(Errors.USELESS_ELVIS.on(expression, baseExpressionType))
                        }
                    }
                    JetTokens.EQEQ,
                    JetTokens.EXCLEQ,
                    JetTokens.EQEQEQ,
                    JetTokens.EXCLEQEQEQ -> {
                        if (expression.getLeft() != null && expression.getRight() != null) {
                            SenselessComparisonChecker.checkSenselessComparisonWithNull(
                                    expression, expression.getLeft()!!, expression.getRight()!!, c,
                                    { c.trace.getType(it) },
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
        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiverArgument, c)
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
            doIfNotNull(dataFlowValue, c) {
                c.trace.report(Errors.UNNECESSARY_SAFE_CALL.on(c.call.getCallOperationNode()!!.getPsi(), receiverArgument.getType()))
            }
        }
    }
}
