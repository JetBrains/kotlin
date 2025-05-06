/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.jvm.lower.*

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

    ::JvmInlineCallableReferenceToLambdaPhase,
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

    ::JvmLocalClassPopupLowering,
    ::StaticCallableReferenceLowering,

    ::JvmDefaultConstructorLowering,

    ::FlattenStringConcatenationLowering,
    ::JvmStringConcatenationLowering,

    ::JvmDefaultArgumentStubGenerator,
    ::JvmDefaultParameterInjector,
    ::JvmDefaultParameterCleaner,

    ::InterfaceLowering,
    ::InheritedDefaultMethodsOnClassesLowering,
    ::GenerateJvmDefaultCompatibilityBridges,
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
    ::ReplaceNumberToCharCallSitesLowering,

    ::RenameFieldsLowering,
    ::FakeLocalVariablesForBytecodeInlinerLowering,
    ::FakeLocalVariablesForIrInlinerLowering,

    ::SpecialAccessLowering,
)

val jvmLoweringPhases = createModulePhases(
    ::ExternalPackageParentPatcherLowering,
    ::FragmentSharedVariablesLowering,
    ::JvmIrValidationBeforeLoweringPhase,
    ::ProcessOptionalAnnotations,
    ::JvmExpectDeclarationRemover,
    ::ConstEvaluationLowering,
    ::SerializeIrPhase,
    ::FileClassLowering,
    ::JvmStaticInObjectLowering,
    ::RepeatedAnnotationLowering,
    ::JvmInlineCallableReferenceToLambdaWithDefaultsPhase,
    ::JvmIrInliner,
    ::ApiVersionIsAtLeastEvaluationLowering,
    ::CreateSeparateCallForInlinedLambdasLowering,
    ::MarkNecessaryInlinedClassesAsRegeneratedLowering,
    ::InlinedClassReferencesBoxingLowering,
    ::RestoreInlineLambda,
) + PerformByIrFilePhase(jvmFilePhases) + createModulePhases(
    ::GenerateMultifileFacades,
    ::ResolveInlineCalls,
    ::JvmIrValidationAfterLoweringPhase
)
