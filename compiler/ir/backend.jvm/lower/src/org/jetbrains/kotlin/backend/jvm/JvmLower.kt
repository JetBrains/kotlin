/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.checkDeclarationParents
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.loops.forLoopsPhase
import org.jetbrains.kotlin.backend.common.lower.optimizations.foldConstantLoweringPhase
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.jvm.ir.constantValue
import org.jetbrains.kotlin.backend.jvm.ir.shouldContainSuspendMarkers
import org.jetbrains.kotlin.backend.jvm.lower.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.PatchDeclarationParentsVisitor
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.NameUtils

private var patchParentPhases = 0

@Suppress("unused")
private fun makePatchParentsPhase(): NamedCompilerPhase<CommonBackendContext, IrFile> {
    val number = patchParentPhases++
    return makeIrFilePhase(
        { PatchDeclarationParents() },
        name = "PatchParents$number",
        description = "Patch parent references in IrFile, pass $number",
    )
}

private var checkParentPhases = 0

@Suppress("unused")
private fun makeCheckParentsPhase(): NamedCompilerPhase<CommonBackendContext, IrFile> {
    val number = checkParentPhases++
    return makeIrFilePhase(
        { CheckDeclarationParents() },
        name = "CheckParents$number",
        description = "Check parent references in IrFile, pass $number",
    )
}

private class PatchDeclarationParents : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(PatchDeclarationParentsVisitor())
    }
}

private class CheckDeclarationParents : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.checkDeclarationParents()
    }
}

private val validateIrBeforeLowering = makeCustomPhase(
    ::validateIr,
    name = "ValidateIrBeforeLowering",
    description = "Validate IR before lowering"
)

private val validateIrAfterLowering = makeCustomPhase(
    ::validateIr,
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

internal val expectDeclarationsRemovingPhase = makeIrModulePhase<CommonBackendContext>(
    ::ExpectDeclarationRemover,
    name = "ExpectDeclarationsRemoving",
    description = "Remove expect declaration from module fragment"
)


internal val propertiesPhase = makeIrFilePhase(
    ::JvmPropertiesLowering,
    name = "Properties",
    description = "Move fields and accessors for properties to their classes, " +
            "replace calls to default property accessors with field accesses, " +
            "remove unused accessors and create synthetic methods for property annotations",
    stickyPostconditions = setOf(PropertiesLowering.Companion::checkNoProperties)
)

internal val IrClass.isGeneratedLambdaClass: Boolean
    get() = origin == JvmLoweredDeclarationOrigin.LAMBDA_IMPL ||
            origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA ||
            origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL ||
            origin == JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE

internal val localDeclarationsPhase = makeIrFilePhase(
    { context ->
        LocalDeclarationsLowering(
            context,
            object : LocalNameProvider {
                override fun localName(declaration: IrDeclarationWithName): String =
                    NameUtils.sanitizeAsJavaIdentifier(super.localName(declaration))
            },
            object : VisibilityPolicy {
                // Note: any condition that results in non-`LOCAL` visibility here should be duplicated in `JvmLocalClassPopupLowering`,
                // else it won't detect the class as local.
                override fun forClass(declaration: IrClass, inInlineFunctionScope: Boolean): DescriptorVisibility =
                    if (declaration.isGeneratedLambdaClass) {
                        scopedVisibility(inInlineFunctionScope)
                    } else {
                        declaration.visibility
                    }

                override fun forConstructor(declaration: IrConstructor, inInlineFunctionScope: Boolean): DescriptorVisibility =
                    if (declaration.parentAsClass.isAnonymousObject)
                        scopedVisibility(inInlineFunctionScope)
                    else
                        declaration.visibility

                override fun forCapturedField(value: IrValueSymbol): DescriptorVisibility =
                    JavaDescriptorVisibilities.PACKAGE_VISIBILITY // avoid requiring a synthetic accessor for it

                private fun scopedVisibility(inInlineFunctionScope: Boolean): DescriptorVisibility =
                    if (inInlineFunctionScope) DescriptorVisibilities.PUBLIC else JavaDescriptorVisibilities.PACKAGE_VISIBILITY
            },
            forceFieldsForInlineCaptures = true
        )
    },
    name = "JvmLocalDeclarations",
    description = "Move local declarations to classes",
    prerequisite = setOf(functionReferencePhase, sharedVariablesPhase)
)

private val jvmLocalClassExtractionPhase = makeIrFilePhase(
    ::JvmLocalClassPopupLowering,
    name = "JvmLocalClassExtraction",
    description = "Move local classes from field initializers and anonymous init blocks into the containing class"
)

private val computeStringTrimPhase = makeIrFilePhase<JvmBackendContext>(
    { context ->
        if (context.state.canReplaceStdlibRuntimeApiBehavior)
            StringTrimLowering(context)
        else
            FileLoweringPass.Empty
    },
    name = "StringTrimLowering",
    description = "Compute trimIndent and trimMargin operations on constant strings"
)

private val defaultArgumentStubPhase = makeIrFilePhase(
    ::JvmDefaultArgumentStubGenerator,
    name = "DefaultArgumentsStubGenerator",
    description = "Generate synthetic stubs for functions with default parameter values",
    prerequisite = setOf(localDeclarationsPhase)
)

private val defaultArgumentCleanerPhase = makeIrFilePhase(
    { context: JvmBackendContext -> DefaultParameterCleaner(context, replaceDefaultValuesWithStubs = true) },
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
    { context -> InnerClassesLowering(context, context.innerClassesSupport) },
    name = "InnerClasses",
    description = "Add 'outer this' fields to inner classes",
    prerequisite = setOf(localDeclarationsPhase)
)

private val innerClassesMemberBodyPhase = makeIrFilePhase(
    { context -> InnerClassesMemberBodyLowering(context, context.innerClassesSupport) },
    name = "InnerClassesMemberBody",
    description = "Replace `this` with 'outer this' field references",
    prerequisite = setOf(innerClassesPhase)
)

private val innerClassConstructorCallsPhase = makeIrFilePhase<JvmBackendContext>(
    { context -> InnerClassConstructorCallsLowering(context, context.innerClassesSupport) },
    name = "InnerClassConstructorCalls",
    description = "Handle constructor calls for inner classes"
)

private val staticInitializersPhase = makeIrFilePhase(
    ::StaticInitializersLowering,
    name = "StaticInitializers",
    description = "Move code from object init blocks and static field initializers to a new <clinit> function"
)

private val initializersPhase = makeIrFilePhase(
    ::InitializersLowering,
    name = "Initializers",
    description = "Merge init blocks and field initializers into constructors",
    // Depends on local class extraction, because otherwise local classes in initializers will be copied into each constructor.
    prerequisite = setOf(jvmLocalClassExtractionPhase)
)

private val initializersCleanupPhase = makeIrFilePhase(
    { context ->
        InitializersCleanupLowering(context) {
            it.constantValue() == null && (!it.isStatic || it.correspondingPropertySymbol?.owner?.isConst != true)
        }
    },
    name = "InitializersCleanup",
    description = "Remove non-static anonymous initializers and non-constant non-static field init expressions",
    stickyPostconditions = setOf(fun(irFile: IrFile) {
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
                error("No anonymous initializers should remain at this stage")
            }
        })
    }),
    prerequisite = setOf(initializersPhase)
)

private val returnableBlocksPhase = makeIrFilePhase(
    ::ReturnableBlockLowering,
    name = "ReturnableBlock",
    description = "Replace returnable blocks with do-while(false) loops",
    prerequisite = setOf(arrayConstructorPhase, assertionPhase)
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

private val kotlinNothingValueExceptionPhase = makeIrFilePhase<CommonBackendContext>(
    { context -> KotlinNothingValueExceptionLowering(context) { it is IrFunction && !it.shouldContainSuspendMarkers() } },
    name = "KotlinNothingValueException",
    description = "Throw proper exception for calls returning value of type 'kotlin.Nothing'"
)

private val jvmFilePhases = listOf(
    jvmClassPreprocessingPhase,
    provisionalFunctionExpressionPhase,

    inventNamesForLocalClassesPhase,
    kCallableNamePropertyPhase,
    annotationImplementationPhase,
    polymorphicSignaturePhase,
    varargPhase,

    jvmLateinitLowering,

    inlineCallableReferenceToLambdaPhase,
    functionReferencePhase,
    suspendLambdaPhase,
    propertyReferenceDelegationPhase,
    propertyReferencePhase,
    arrayConstructorPhase,
    constPhase1,

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
    jvmInlineClassPhase,
    tailrecPhase,
    // makePatchParentsPhase(),

    enumWhenPhase,
    singletonReferencesPhase,

    assertionPhase,
    returnableBlocksPhase,
    sharedVariablesPhase,
    localDeclarationsPhase,
    // makePatchParentsPhase(),

    jvmLocalClassExtractionPhase,
    staticCallableReferencePhase,

    flattenStringConcatenationPhase,
    foldConstantLoweringPhase,
    computeStringTrimPhase,
    jvmStringConcatenationLowering,

    defaultArgumentStubPhase,
    defaultArgumentInjectorPhase,
    defaultArgumentCleanerPhase,

    // makePatchParentsPhase(),
    interfacePhase,
    inheritedDefaultMethodsOnClassesPhase,
    replaceDefaultImplsOverriddenSymbolsPhase,
    interfaceSuperCallsPhase,
    interfaceDefaultCallsPhase,
    interfaceObjectCallsPhase,

    tailCallOptimizationPhase,
    addContinuationPhase,
    constPhase2, // handle const properties in default arguments of "original" suspend funs

    innerClassesPhase,
    innerClassesMemberBodyPhase,
    innerClassConstructorCallsPhase,

    // makePatchParentsPhase(),

    enumClassPhase,
    objectClassPhase,
    staticInitializersPhase,
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
    typeOperatorLowering,
    replaceKFunctionInvokeWithFunctionInvokePhase,
    kotlinNothingValueExceptionPhase,
    makePropertyDelegateMethodsStaticPhase,

    renameFieldsPhase,
    fakeInliningLocalVariablesLowering,

    // makePatchParentsPhase()
)

val jvmLoweringPhases = NamedCompilerPhase(
    name = "IrLowering",
    description = "IR lowering",
    nlevels = 1,
    actions = setOf(defaultDumper, validationAction),
    lower = validateIrBeforeLowering then
            processOptionalAnnotationsPhase then
            expectDeclarationsRemovingPhase then
            serializeIrPhase then
            scriptsToClassesPhase then
            fileClassPhase then
            jvmStaticInObjectPhase then
            repeatedAnnotationPhase then
            performByIrFile(lower = jvmFilePhases) then
            generateMultifileFacadesPhase then
            resolveInlineCallsPhase then
            // should be last transformation
            prepareForBytecodeInlining then
            validateIrAfterLowering
)
