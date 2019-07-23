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
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.PatchDeclarationParentsVisitor
import org.jetbrains.kotlin.ir.visitors.acceptVoid
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
    ::LateinitLowering,
    name = "Lateinit",
    description = "Insert checks for lateinit field references"
)

private val propertiesPhase = makeIrFilePhase<CommonBackendContext>(
    { context ->
        PropertiesLowering(context, JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS) { propertyName ->
            JvmAbi.getSyntheticMethodNameForAnnotatedProperty(propertyName)
        }
    },
    name = "Properties",
    description = "Move fields and accessors for properties to their classes",
    stickyPostconditions = setOf((PropertiesLowering)::checkNoProperties)
)

private val localDeclarationsPhase = makeIrFilePhase<CommonBackendContext>(
    { context ->
        LocalDeclarationsLowering(context, object : LocalNameProvider {
            override fun localName(declaration: IrDeclarationWithName): String =
                NameUtils.sanitizeAsJavaIdentifier(super.localName(declaration))
        }, Visibilities.PUBLIC)
    },
    name = "JvmLocalDeclarations",
    description = "Move local declarations to classes",
    prerequisite = setOf(sharedVariablesPhase)
)

private val defaultArgumentStubPhase = makeIrFilePhase<CommonBackendContext>(
    { context -> DefaultArgumentStubGenerator(context, false) },
    name = "DefaultArgumentsStubGenerator",
    description = "Generate synthetic stubs for functions with default parameter values",
    prerequisite = setOf(localDeclarationsPhase)
)

private val innerClassesPhase = makeIrFilePhase(
    ::InnerClassesLowering,
    name = "InnerClasses",
    description = "Add 'outer this' fields to inner classes",
    prerequisite = setOf(localDeclarationsPhase)
)

private val jvmFilePhases =
        provisionalFunctionExpressionPhase then
        inventNamesForLocalClassesPhase then
        kCallableNamePropertyPhase then
        arrayConstructorPhase then

        lateinitPhase then

        moveOrCopyCompanionObjectFieldsPhase then
        inlineCallableReferenceToLambdaPhase then
        propertyReferencePhase then
        constPhase then
        propertiesToFieldsPhase then
        propertiesPhase then
        renameFieldsPhase then
        annotationPhase then
        tailrecPhase then

        jvmInlineClassPhase then

        sharedVariablesPhase then

        makePatchParentsPhase(1) then

        enumWhenPhase then
        singletonReferencesPhase then
        localDeclarationsPhase then
        defaultArgumentStubPhase then

        interfacePhase then
        interfaceDelegationPhase then
        interfaceSuperCallsPhase then

        singleAbstractMethodPhase then
        addContinuationPhase then
        callableReferencePhase then
        functionNVarargInvokePhase then

        innerClassesPhase then
        innerClassConstructorCallsPhase then
        forLoopsPhase then

        makePatchParentsPhase(2) then

        enumClassPhase then
        objectClassPhase then
        makeInitializersPhase(JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER, true) then
        syntheticAccessorPhase then
        collectionStubMethodLowering then
        bridgePhase then
        jvmOverloadsAnnotationPhase then
        jvmDefaultConstructorPhase then
        jvmStaticAnnotationPhase then
        staticDefaultFunctionPhase then

        toArrayPhase then
        flattenStringConcatenationPhase then
        foldConstantLoweringPhase then
        computeStringTrimPhase then
        jvmBuiltinOptimizationLoweringPhase then
        additionalClassAnnotationPhase then

        recordNamesForKotlinTypeMapperPhase then

        // should be last transformation
        removeDeclarationsThatWouldBeInlined then
        makePatchParentsPhase(3)

val jvmPhases = namedIrModulePhase(
    name = "IrLowering",
    description = "IR lowering",
    lower = expectDeclarationsRemovingPhase then
            fileClassPhase then
            performByIrFile(lower = jvmFilePhases)
)

class JvmLower(val context: JvmBackendContext) {
    fun lower(irModuleFragment: IrModuleFragment) {
        // TODO run lowering passes as callbacks in bottom-up visitor
        jvmPhases.invokeToplevel(context.phaseConfig, context, irModuleFragment)
    }
}
