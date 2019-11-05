/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.loops.forLoopsPhase
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.jvm.lower.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.PatchDeclarationParentsVisitor
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.NameUtils

private fun makePatchParentsPhase(number: Int) = namedIrFilePhase(
    lower = object : SameTypeCompilerPhase<CommonBackendContext, IrFile> {
        override fun invoke(
            phaseConfig: PhaseConfig,
            phaserState: PhaserState<IrFile>,
            context: CommonBackendContext,
            input: IrFile
        ): IrFile {
            input.acceptVoid(PatchDeclarationParentsVisitor())
            return input
        }
    },
    name = "PatchParents$number",
    description = "Patch parent references in IrFile, pass $number",
    nlevels = 0
)

private val validateIrBeforeLowering = makeCustomPhase<JvmBackendContext, IrModuleFragment>(
    { context, module -> validationCallback(context, module) },
    name = "ValidateIrBeforeLowering",
    description = "Validate IR before lowering"
)

private val validateIrAfterLowering = makeCustomPhase<JvmBackendContext, IrModuleFragment>(
    { context, module -> validationCallback(context, module) },
    name = "ValidateIrAfterLowering",
    description = "Validate IR after lowering"
)

private val stripTypeAliasDeclarationsPhase = makeIrFilePhase<CommonBackendContext>(
    { StripTypeAliasDeclarationsLowering() },
    name = "StripTypeAliasDeclarations",
    description = "Strip typealias declarations"
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

private val expectDeclarationsRemovingPhase = makeIrModulePhase(
    ::ExpectDeclarationsRemoveLowering,
    name = "ExpectDeclarationsRemoving",
    description = "Remove expect declaration from module fragment"
)

private val lateinitPhase = makeIrFilePhase(
    { backend: JvmBackendContext ->
        object : LateinitLowering(backend) {
            override fun throwUninitializedPropertyAccessException(builder: IrBuilderWithScope, name: String): IrExpression =
                builder.irBlock {
                    +super.throwUninitializedPropertyAccessException(builder, name)
                    +irThrow(irNull())
                }
        }
    },
    name = "Lateinit",
    description = "Insert checks for lateinit field references"
)

private val propertiesPhase = makeIrFilePhase<JvmBackendContext>(
    { context ->
        PropertiesLowering(context, JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS) { property ->
            val baseName =
                if (context.state.languageVersionSettings.supportsFeature(LanguageFeature.UseGetterNameForPropertyAnnotationsMethodOnJvm)) {
                    property.getter?.let { getter ->
                        context.methodSignatureMapper.mapFunctionName(getter)
                    } ?: JvmAbi.getterName(property.name.asString())
                } else {
                    property.name.asString()
                }
            JvmAbi.getSyntheticMethodNameForAnnotatedProperty(baseName)
        }
    },
    name = "Properties",
    description = "Move fields and accessors for properties to their classes",
    stickyPostconditions = setOf((PropertiesLowering)::checkNoProperties)
)

internal val localDeclarationsPhase = makeIrFilePhase<CommonBackendContext>(
    { context ->
        LocalDeclarationsLowering(
            context,
            object : LocalNameProvider {
                override fun localName(declaration: IrDeclarationWithName): String =
                    NameUtils.sanitizeAsJavaIdentifier(super.localName(declaration))
            },
            object : VisibilityPolicy {
                override fun forClass(declaration: IrClass, inInlineFunctionScope: Boolean): Visibility =
                    if (declaration.origin == JvmLoweredDeclarationOrigin.LAMBDA_IMPL ||
                        declaration.origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL ||
                        declaration.origin == JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE) {
                        scopedVisibility(inInlineFunctionScope)
                    } else {
                        declaration.visibility
                    }

                override fun forConstructor(declaration: IrConstructor, inInlineFunctionScope: Boolean): Visibility =
                    scopedVisibility(inInlineFunctionScope)

                private fun scopedVisibility(inInlineFunctionScope: Boolean): Visibility =
                    if (inInlineFunctionScope) Visibilities.PUBLIC else JavaVisibilities.PACKAGE_VISIBILITY
            }
        )
    },
    name = "JvmLocalDeclarations",
    description = "Move local declarations to classes",
    prerequisite = setOf(callableReferencePhase, sharedVariablesPhase)
)

private val defaultArgumentStubPhase = makeIrFilePhase(
    ::JvmDefaultArgumentStubGenerator,
    name = "DefaultArgumentsStubGenerator",
    description = "Generate synthetic stubs for functions with default parameter values",
    prerequisite = setOf(localDeclarationsPhase)
)

private val defaultArgumentInjectorPhase = makeIrFilePhase(
    ::JvmDefaultParameterInjector,
    name = "DefaultParameterInjector",
    description = "Transform calls with default arguments into calls to stubs",
    prerequisite = setOf(defaultArgumentStubPhase, callableReferencePhase, inlineCallableReferenceToLambdaPhase)
)

private val interfacePhase = makeIrFilePhase(
    ::InterfaceLowering,
    name = "Interface",
    description = "Move default implementations of interface members to DefaultImpls class",
    prerequisite = setOf(defaultArgumentInjectorPhase)
)

private val innerClassesPhase = makeIrFilePhase(
    ::InnerClassesLowering,
    name = "InnerClasses",
    description = "Add 'outer this' fields to inner classes",
    prerequisite = setOf(localDeclarationsPhase)
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

private val mainMethodGenerationPhase = makeIrFilePhase(
    ::MainMethodGenerationLowering,
    name = "MainMethodGeneration",
    description = "Identify parameterless main methods and generate bridge main-methods"
)

@Suppress("Reformat")
private val jvmFilePhases =
        mainMethodGenerationPhase then
        typeAliasAnnotationMethodsPhase then
        stripTypeAliasDeclarationsPhase then
        provisionalFunctionExpressionPhase then
        inventNamesForLocalClassesPhase then
        kCallableNamePropertyPhase then
        annotationPhase then
        varargPhase then
        arrayConstructorPhase then
        checkNotNullPhase then

        lateinitPhase then

        moveOrCopyCompanionObjectFieldsPhase then
        inlineCallableReferenceToLambdaPhase then
        propertyReferencePhase then
        constPhase then
        propertiesToFieldsPhase then
        propertiesPhase then
        renameFieldsPhase then
        anonymousObjectSuperConstructorPhase then
        tailrecPhase then

        jvmInlineClassPhase then

        sharedVariablesPhase then

        makePatchParentsPhase(1) then

        enumWhenPhase then
        singletonReferencesPhase then

        callableReferencePhase then
        singleAbstractMethodPhase then
        assertionPhase then
        returnableBlocksPhase then
        localDeclarationsPhase then

        jvmOverloadsAnnotationPhase then
        jvmDefaultConstructorPhase then

        forLoopsPhase then
        flattenStringConcatenationPhase then
        foldConstantLoweringPhase then
        computeStringTrimPhase then
        jvmStringConcatenationLowering then

        defaultArgumentStubPhase then
        defaultArgumentInjectorPhase then

        interfacePhase then
        interfaceDelegationPhase then
        interfaceSuperCallsPhase then
        interfaceDefaultCallsPhase then

        addContinuationPhase then

        innerClassesPhase then
        innerClassConstructorCallsPhase then

        makePatchParentsPhase(2) then

        enumClassPhase then
        objectClassPhase then
        makeInitializersPhase(JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER, true) then
        collectionStubMethodLowering then
        functionNVarargBridgePhase then
        bridgePhase then
        jvmStaticAnnotationPhase then
        staticDefaultFunctionPhase then
        syntheticAccessorPhase then

        toArrayPhase then
        jvmBuiltinOptimizationLoweringPhase then
        additionalClassAnnotationPhase then
        typeOperatorLowering then
        replaceKFunctionInvokeWithFunctionInvokePhase then

        checkLocalNamesWithOldBackendPhase then

        // should be last transformation
        removeDeclarationsThatWouldBeInlined then
        makePatchParentsPhase(3)

val jvmPhases = namedIrModulePhase(
    name = "IrLowering",
    description = "IR lowering",
    lower = validateIrBeforeLowering then
            expectDeclarationsRemovingPhase then
            fileClassPhase then
            performByIrFile(lower = jvmFilePhases) then
            generateMultifileFacadesPhase then
            validateIrAfterLowering
)

class JvmLower(val context: JvmBackendContext) {
    fun lower(irModuleFragment: IrModuleFragment) {
        // TODO run lowering passes as callbacks in bottom-up visitor
        jvmPhases.invokeToplevel(context.phaseConfig, context, irModuleFragment)
    }
}
