/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.loops.forLoopsPhase
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.jvm.lower.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.util.isExpect

internal fun JvmBackendContext.irInlinerIsEnabled(): Boolean =
    config.enableIrInliner

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

// TODO make all lambda-related stuff work with IrFunctionExpression and drop this phase
private val provisionalFunctionExpressionPhase = makeIrFilePhase<CommonBackendContext>(
    { ProvisionalFunctionExpressionLowering() },
    name = "FunctionExpression",
    description = "Transform IrFunctionExpression to a local function reference"
)

private val arrayConstructorPhase = makeIrFilePhase(
    ::ArrayConstructorLowering,
    name = "ArrayConstructor",
    description = "Transform `Array(size) { index -> value }` into a loop"
)

internal val expectDeclarationsRemovingPhase = makeIrModulePhase(
    { context: JvmBackendContext ->
        if (context.config.useFir) object : FileLoweringPass {
            override fun lower(irFile: IrFile) {
                irFile.declarations.removeIf { it.isExpect }
            }
        }
        else ExpectDeclarationRemover(context)
    },
    name = "ExpectDeclarationsRemoving",
    description = "Remove expect declaration from module fragment"
)


internal val propertiesPhase = makeIrFilePhase(
    ::JvmPropertiesLowering,
    name = "Properties",
    description = "Move fields and accessors for properties to their classes, " +
            "replace calls to default property accessors with field accesses, " +
            "remove unused accessors and create synthetic methods for property annotations",
)

internal val localDeclarationsPhase = makeIrFilePhase(
    ::JvmLocalDeclarationsLowering,
    name = "JvmLocalDeclarations",
    description = "Move local declarations to classes",
    prerequisite = setOf(functionReferencePhase, sharedVariablesPhase)
)

private val jvmLocalClassExtractionPhase = makeIrFilePhase(
    ::JvmLocalClassPopupLowering,
    name = "JvmLocalClassExtraction",
    description = "Move local classes from field initializers and anonymous init blocks into the containing class"
)

private val defaultArgumentStubPhase = makeIrFilePhase(
    ::JvmDefaultArgumentStubGenerator,
    name = "DefaultArgumentsStubGenerator",
    description = "Generate synthetic stubs for functions with default parameter values",
    prerequisite = setOf(localDeclarationsPhase)
)

val defaultArgumentCleanerPhase = makeIrFilePhase(
    ::JvmDefaultParameterCleaner,
    name = "DefaultParameterCleaner",
    description = "Replace default values arguments with stubs",
    prerequisite = setOf(defaultArgumentStubPhase)
)

private val defaultArgumentInjectorPhase = makeIrFilePhase(
    ::JvmDefaultParameterInjector,
    name = "DefaultParameterInjector",
    description = "Transform calls with default arguments into calls to stubs",
    prerequisite = setOf(functionReferencePhase, inlineCallableReferenceToLambdaPhase)
)

private val interfacePhase = makeIrFilePhase(
    ::InterfaceLowering,
    name = "Interface",
    description = "Move default implementations of interface members to DefaultImpls class",
    prerequisite = setOf(defaultArgumentInjectorPhase)
)

private val innerClassesPhase = makeIrFilePhase(
    ::JvmInnerClassesLowering,
    name = "InnerClasses",
    description = "Add 'outer this' fields to inner classes",
    prerequisite = setOf(localDeclarationsPhase)
)

private val innerClassesMemberBodyPhase = makeIrFilePhase(
    ::JvmInnerClassesMemberBodyLowering,
    name = "InnerClassesMemberBody",
    description = "Replace `this` with 'outer this' field references",
    prerequisite = setOf(innerClassesPhase)
)

private val innerClassConstructorCallsPhase = makeIrFilePhase(
    ::JvmInnerClassConstructorCallsLowering,
    name = "InnerClassConstructorCalls",
    description = "Handle constructor calls for inner classes"
)

private val staticInitializersPhase = makeIrFilePhase(
    ::StaticInitializersLowering,
    name = "StaticInitializers",
    description = "Move code from object init blocks and static field initializers to a new <clinit> function"
)

private val initializersPhase = makeIrFilePhase(
    ::JvmInitializersLowering,
    name = "Initializers",
    description = "Merge init blocks and field initializers into constructors",
    // Depends on local class extraction, because otherwise local classes in initializers will be copied into each constructor.
    prerequisite = setOf(jvmLocalClassExtractionPhase)
)

private val initializersCleanupPhase = makeIrFilePhase(
    ::JvmInitializersCleanupLowering,
    name = "InitializersCleanup",
    description = "Remove non-static anonymous initializers and non-constant non-static field init expressions",
    prerequisite = setOf(initializersPhase)
)

private val returnableBlocksPhase = makeIrFilePhase(
    ::JvmReturnableBlockLowering,
    name = "ReturnableBlock",
    description = "Replace returnable blocks with do-while(false) loops",
    prerequisite = setOf(arrayConstructorPhase, assertionPhase, directInvokeLowering)
)

private val singletonReferencesPhase = makeIrFilePhase(
    ::SingletonReferencesLowering,
    name = "SingletonReferences",
    description = "Handle singleton references",
    // ReturnableBlock lowering may produce references to the `Unit` object
    prerequisite = setOf(returnableBlocksPhase)
)

private val syntheticAccessorPhase = makeIrFilePhase(
    ::SyntheticAccessorLowering,
    name = "SyntheticAccessor",
    description = "Introduce synthetic accessors",
    prerequisite = setOf(objectClassPhase, staticDefaultFunctionPhase, interfacePhase)
)

private val tailrecPhase = makeIrFilePhase(
    ::JvmTailrecLowering,
    name = "Tailrec",
    description = "Handle tailrec calls",
)

private val kotlinNothingValueExceptionPhase = makeIrFilePhase(
    ::JvmKotlinNothingValueExceptionLowering,
    name = "KotlinNothingValueException",
    description = "Throw proper exception for calls returning value of type 'kotlin.Nothing'"
)

internal val functionInliningPhase = makeIrModulePhase(
    { context ->
        if (!context.irInlinerIsEnabled()) return@makeIrModulePhase FileLoweringPass.Empty

        FunctionInlining(
            context,
            innerClassesSupport = context.innerClassesSupport,
            regenerateInlinedAnonymousObjects = true
        )
    },
    name = "FunctionInliningPhase",
    description = "Perform function inlining",
    prerequisite = setOf(
        expectDeclarationsRemovingPhase,
    )
)

private val apiVersionIsAtLeastEvaluationPhase = makeIrModulePhase(
    { context ->
        if (!context.irInlinerIsEnabled()) return@makeIrModulePhase FileLoweringPass.Empty
        ApiVersionIsAtLeastEvaluationLowering(context)
    },
    name = "ApiVersionIsAtLeastEvaluationLowering",
    description = "Evaluate inlined invocations of `apiVersionIsAtLeast`",
    prerequisite = setOf(functionInliningPhase)
)

private val inlinedClassReferencesBoxingPhase = makeIrModulePhase(
    { context ->
        if (!context.irInlinerIsEnabled()) return@makeIrModulePhase FileLoweringPass.Empty
        InlinedClassReferencesBoxingLowering(context)
    },
    name = "InlinedClassReferencesBoxingLowering",
    description = "Replace inlined primitive types in class references with boxed versions",
    prerequisite = setOf(functionInliningPhase, markNecessaryInlinedClassesAsRegenerated)
)

private val constEvaluationPhase = makeIrModulePhase<JvmBackendContext>(
    ::ConstEvaluationLowering,
    name = "ConstEvaluationLowering",
    description = "Evaluate functions that are marked as `IntrinsicConstEvaluation`"
)

private val jvmFilePhases = listOf(
    typeAliasAnnotationMethodsPhase,
    provisionalFunctionExpressionPhase,

    jvmOverloadsAnnotationPhase,
    mainMethodGenerationPhase,

    annotationPhase,
    annotationImplementationPhase,
    polymorphicSignaturePhase,
    varargPhase,

    jvmLateinitLowering,
    inventNamesForLocalClassesPhase,

    inlineCallableReferenceToLambdaPhase,
    directInvokeLowering,
    functionReferencePhase,

    suspendLambdaPhase,
    propertyReferenceDelegationPhase,
    singletonOrConstantDelegationPhase,
    propertyReferencePhase,
    arrayConstructorPhase,

    // TODO: merge the next three phases together, as visitors behave incorrectly between them
    //  (backing fields moved out of companion objects are reachable by two paths):
    moveOrCopyCompanionObjectFieldsPhase,
    propertiesPhase,
    remapObjectFieldAccesses,

    anonymousObjectSuperConstructorPhase,
    jvmBuiltInsPhase,

    rangeContainsLoweringPhase,
    forLoopsPhase,
    collectionStubMethodLowering,
    singleAbstractMethodPhase,
    jvmMultiFieldValueClassPhase,
    jvmInlineClassPhase,
    tailrecPhase,

    enumWhenPhase,

    assertionPhase,
    returnableBlocksPhase,
    singletonReferencesPhase,
    sharedVariablesPhase,
    localDeclarationsPhase,

    removeDuplicatedInlinedLocalClasses,
    inventNamesForInlinedLocalClassesPhase,

    jvmLocalClassExtractionPhase,
    staticCallableReferencePhase,

    jvmDefaultConstructorPhase,

    flattenStringConcatenationPhase,
    jvmStringConcatenationLowering,

    defaultArgumentStubPhase,
    defaultArgumentInjectorPhase,
    defaultArgumentCleanerPhase,

    interfacePhase,
    inheritedDefaultMethodsOnClassesPhase,
    replaceDefaultImplsOverriddenSymbolsPhase,
    interfaceSuperCallsPhase,
    interfaceDefaultCallsPhase,
    interfaceObjectCallsPhase,

    tailCallOptimizationPhase,
    addContinuationPhase,

    innerClassesPhase,
    innerClassesMemberBodyPhase,
    innerClassConstructorCallsPhase,

    enumClassPhase,
    enumExternalEntriesPhase,
    objectClassPhase,
    staticInitializersPhase,
    uniqueLoopLabelsPhase,
    initializersPhase,
    initializersCleanupPhase,
    functionNVarargBridgePhase,
    jvmStaticInCompanionPhase,
    staticDefaultFunctionPhase,
    bridgePhase,
    syntheticAccessorPhase,

    jvmArgumentNullabilityAssertions,
    toArrayPhase,
    jvmSafeCallFoldingPhase,
    jvmOptimizationLoweringPhase,
    additionalClassAnnotationPhase,
    recordEnclosingMethodsPhase,
    typeOperatorLowering,
    replaceKFunctionInvokeWithFunctionInvokePhase,
    kotlinNothingValueExceptionPhase,
    makePropertyDelegateMethodsStaticPhase,
    addSuperQualifierToJavaFieldAccessPhase,
    replaceNumberToCharCallSitesPhase,

    renameFieldsPhase,
    fakeLocalVariablesForBytecodeInlinerLowering,
    fakeLocalVariablesForIrInlinerLowering,
)

val jvmLoweringPhases = buildJvmLoweringPhases("IrLowering", listOf("PerformByIrFile" to jvmFilePhases))

private fun buildJvmLoweringPhases(
    name: String,
    phases: List<Pair<String, List<SimpleNamedCompilerPhase<JvmBackendContext, IrFile, IrFile>>>>,
): SameTypeNamedCompilerPhase<JvmBackendContext, IrModuleFragment> {
    return SameTypeNamedCompilerPhase(
        name = name,
        description = "IR lowering",
        nlevels = 1,
        actions = setOf(defaultDumper, validationAction),
        lower =
        externalPackageParentPatcherPhase then
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

                buildLoweringsPhase(phases) then
                generateMultifileFacadesPhase then
                resolveInlineCallsPhase then
                validateIrAfterLowering
    )
}

// Build a compiler phase from a list of lowering sequences: each subsequence is run
// in parallel per file, and each parallel composition is run in sequence.
private fun buildLoweringsPhase(
    perModuleLowerings: List<Pair<String, List<SimpleNamedCompilerPhase<JvmBackendContext, IrFile, IrFile>>>>,
): CompilerPhase<JvmBackendContext, IrModuleFragment, IrModuleFragment> =
    perModuleLowerings.map { (name, lowerings) -> performByIrFile(name, lower = lowerings) }
        .reduce<
                CompilerPhase<JvmBackendContext, IrModuleFragment, IrModuleFragment>,
                CompilerPhase<JvmBackendContext, IrModuleFragment, IrModuleFragment>
                > { result, phase -> result then phase }


val jvmFragmentLoweringPhases = run {
    val defaultArgsPhase = jvmFilePhases.indexOf(defaultArgumentCleanerPhase)
    val loweringsUpToAndIncludingDefaultArgsPhase = jvmFilePhases.subList(0, defaultArgsPhase + 1)
    val remainingLowerings = jvmFilePhases.subList(defaultArgsPhase + 1, jvmFilePhases.size)
    buildJvmLoweringPhases(
        "IrFragmentLowering",
        listOf(
            "PrefixOfIRPhases" to loweringsUpToAndIncludingDefaultArgsPhase,
            "FragmentLowerings" to listOf(
                fragmentLocalFunctionPatchLowering,
                reflectiveAccessLowering,
            ),
            "SuffixOfIRPhases" to remainingLowerings
        )
    )
}
