/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.phaser.PerformByIrFilePhase
import org.jetbrains.kotlin.backend.common.phaser.createFilePhases
import org.jetbrains.kotlin.backend.common.phaser.createModulePhases
import org.jetbrains.kotlin.backend.jvm.lower.*
import org.jetbrains.kotlin.config.phaser.AnyNamedPhase

private val jvmModulePhases1 = createModulePhases(
    ::ExternalPackageParentPatcherLowering,
    ::FragmentSharedVariablesLowering,
    ::JvmK1IrValidationBeforeLoweringPhase,
    ::ProcessOptionalAnnotations,
    ::JvmExpectDeclarationRemover,
    ::ConstEvaluationLowering,
    ::FileClassLowering,
    ::JvmStaticInObjectLowering,
    ::RepeatedAnnotationLowering,
)

private val jvmFilePhases = createFilePhases(
    ::TypeAliasAnnotationMethodsLowering,
    ::ProvisionalFunctionExpressionLowering,

    ::JvmVersionOverloadsLowering,
    ::JvmOverloadsAnnotationLowering,
    ::MainMethodGenerationLowering,

    ::AnnotationLowering,
    ::JvmAnnotationImplementationLowering,
    ::PolymorphicSignatureLowering,
    ::VarargLowering,

    ::JvmLateinitLowering,
    ::JvmInventNamesForLocalClasses,

    ::JvmInlineCallableReferenceToLambdaPhase,
    ::JvmUpgradeCallableReferences,
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
    ::JvmInlineMultiFieldValueClassLowering,
    ::JvmInlineClassLowering,
    ::JvmTailrecLowering,

    ::MappedEnumWhenLowering,

    ::AssertionLowering,
    ::JvmReturnableBlockLowering,
    ::SingletonReferencesLowering,
    ::JvmSharedVariablesLowering,

    ::JvmInventNamesForLocalFunctions,
    ::JvmLocalDeclarationsLowering,
    ::JvmLocalDeclarationPopupLowering,

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
    ::IndyLambdaMetafactoryLowering,
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

    ::SpecialAccessLowering,

    ::TypeSwitchLowering,
)

private val jvmModulePhases2 = createModulePhases(
    ::GenerateMultifileFacades,
    ::ResolveInlineCalls,
    ::JvmIrValidationAfterLoweringPhase,
)

val jvmLoweringPhases = jvmModulePhases1 + PerformByIrFilePhase(jvmFilePhases) + jvmModulePhases2

@TestOnly
internal fun getJvmLoweringPhaseListsForTests(): List<List<AnyNamedPhase>> =
    listOf(jvmModulePhases1, jvmFilePhases, jvmModulePhases2)
