/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.jvm.lower.*

private val validateIrBeforeLowering = makeIrModulePhase(
    ::JvmIrValidationBeforeLoweringPhase,
    name = "ValidateIrBeforeLowering",
    description = "Validate IR before lowering"
)

private val validateIrAfterLowering = makeIrModulePhase(
    ::JvmIrValidationAfterLoweringPhase,
    name = "ValidateIrAfterLowering",
    description = "Validate IR after lowering"
)

internal val expectDeclarationsRemovingPhase = makeIrModulePhase(
    ::JvmExpectDeclarationRemover,
    name = "ExpectDeclarationsRemoving",
    description = "Remove expect declaration from module fragment"
)

internal val functionInliningPhase = makeIrModulePhase(
    ::JvmIrInliner,
    name = "FunctionInliningPhase",
    description = "Perform function inlining",
    prerequisite = setOf(expectDeclarationsRemovingPhase)
)

private val apiVersionIsAtLeastEvaluationPhase = makeIrModulePhase(
    ::ApiVersionIsAtLeastEvaluationLowering,
    name = "ApiVersionIsAtLeastEvaluationLowering",
    description = "Evaluate inlined invocations of `apiVersionIsAtLeast`",
    prerequisite = setOf(functionInliningPhase)
)

private val inlinedClassReferencesBoxingPhase = makeIrModulePhase(
    ::InlinedClassReferencesBoxingLowering,
    name = "InlinedClassReferencesBoxingLowering",
    description = "Replace inlined primitive types in class references with boxed versions",
    prerequisite = setOf(functionInliningPhase, markNecessaryInlinedClassesAsRegenerated)
)

private val constEvaluationPhase = makeIrModulePhase<JvmBackendContext>(
    ::ConstEvaluationLowering,
    name = "ConstEvaluationLowering",
    description = "Evaluate functions that are marked as `IntrinsicConstEvaluation`"
)

private val jvmFilePhases = createFilePhases<JvmBackendContext>(
    ::TypeAliasAnnotationMethodsLowering,
    ::ProvisionalFunctionExpressionLowering,

    ::JvmOverloadsAnnotationLowering,
    ::MainMethodGenerationLowering,

    ::AnnotationLowering,
    ::JvmAnnotationImplementationLowering,
    ::PolymorphicSignatureLowering,
    ::VarargLowering,

    ::JvmLateinitLowering,
    ::JvmInventNamesForLocalClasses,

    ::InlineCallableReferenceToLambdaPhase,
    ::DirectInvokeLowering,
    ::FunctionReferenceLowering,

    ::SuspendLambdaLowering,
    ::PropertyReferenceDelegationLowering,
    ::SingletonOrConstantDelegationLowering,
    ::PropertyReferenceLowering,
    ::ArrayConstructorLowering,

    // TODO: merge the next three phases together, as visitors behave incorrectly between them
    //  (backing fields moved out of companion objects are reachable by two paths):
    ::MoveOrCopyCompanionObjectFieldsLowering,
    ::JvmPropertiesLowering,
    ::RemapObjectFieldAccesses,

    ::AnonymousObjectSuperConstructorLowering,
    ::JvmBuiltInsLowering,

    ::RangeContainsLowering,
    ::ForLoopsLowering,
    ::CollectionStubMethodLowering,
    ::JvmSingleAbstractMethodLowering,
    ::JvmMultiFieldValueClassLowering,
    ::JvmInlineClassLowering,
    ::JvmTailrecLowering,

    ::MappedEnumWhenLowering,

    ::AssertionLowering,
    ::JvmReturnableBlockLowering,
    ::SingletonReferencesLowering,
    ::SharedVariablesLowering,
    ::JvmLocalDeclarationsLowering,

    ::RemoveDuplicatedInlinedLocalClassesLowering,
    ::JvmInventNamesForInlinedAnonymousObjects,

    ::JvmLocalClassPopupLowering,
    ::StaticCallableReferenceLowering,

    ::JvmDefaultConstructorLowering,

    ::FlattenStringConcatenationLowering,
    ::JvmStringConcatenationLowering,

    ::JvmDefaultArgumentStubGenerator,
    ::JvmDefaultParameterInjector,
    ::JvmDefaultParameterCleaner,

    ::FragmentLocalFunctionPatchLowering,
    ::ReflectiveAccessLowering,

    ::InterfaceLowering,
    ::InheritedDefaultMethodsOnClassesLowering,
    ::ReplaceDefaultImplsOverriddenSymbols,
    ::InterfaceSuperCallsLowering,
    ::InterfaceDefaultCallsLowering,
    ::InterfaceObjectCallsLowering,

    ::TailCallOptimizationLowering,
    ::AddContinuationLowering,

    ::JvmInnerClassesLowering,
    ::JvmInnerClassesMemberBodyLowering,
    ::JvmInnerClassConstructorCallsLowering,

    ::EnumClassLowering,
    ::EnumExternalEntriesLowering,
    ::ObjectClassLowering,
    ::StaticInitializersLowering,
    ::UniqueLoopLabelsLowering,
    ::JvmInitializersLowering,
    ::JvmInitializersCleanupLowering,
    ::FunctionNVarargBridgeLowering,
    ::JvmStaticInCompanionLowering,
    ::StaticDefaultFunctionLowering,
    ::BridgeLowering,
    ::SyntheticAccessorLowering,

    ::JvmArgumentNullabilityAssertionsLowering,
    ::ToArrayLowering,
    ::JvmSafeCallChainFoldingLowering,
    ::JvmOptimizationLowering,
    ::AdditionalClassAnnotationLowering,
    ::RecordEnclosingMethodsLowering,
    ::TypeOperatorLowering,
    ::ReplaceKFunctionInvokeWithFunctionInvoke,
    ::JvmKotlinNothingValueExceptionLowering,
    ::MakePropertyDelegateMethodsStaticLowering,
    ::AddSuperQualifierToJavaFieldAccessLowering,
    ::ReplaceNumberToCharCallSitesLowering,

    ::RenameFieldsLowering,
    ::FakeLocalVariablesForBytecodeInlinerLowering,
    ::FakeLocalVariablesForIrInlinerLowering,
)

val jvmLoweringPhases = SameTypeNamedCompilerPhase(
    name = "IrLowering",
    description = "IR lowering",
    nlevels = 1,
    actions = setOf(defaultDumper, validationAction),
    lower = externalPackageParentPatcherPhase then
            fragmentSharedVariablesLowering then
            validateIrBeforeLowering then
            processOptionalAnnotationsPhase then
            expectDeclarationsRemovingPhase then
            constEvaluationPhase then
            serializeIrPhase then
            scriptsToClassesPhase then
            fileClassPhase then
            jvmStaticInObjectPhase then
            repeatedAnnotationPhase then

            functionInliningPhase then
            apiVersionIsAtLeastEvaluationPhase then
            createSeparateCallForInlinedLambdas then
            markNecessaryInlinedClassesAsRegenerated then
            inlinedClassReferencesBoxingPhase then

            performByIrFile("PerformByIrFile", lower = jvmFilePhases) then

            generateMultifileFacadesPhase then
            resolveInlineCallsPhase then
            validateIrAfterLowering
)
