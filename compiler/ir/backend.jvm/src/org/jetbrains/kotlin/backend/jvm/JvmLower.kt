/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.loops.forLoopsPhase
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.jvm.lower.*
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.PatchDeclarationParentsVisitor
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.NameUtils

private fun makePatchParentsPhase(number: Int) = namedIrFilePhase(
    lower = object : SameTypeCompilerPhase<CommonBackendContext, IrFile> {
        override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<IrFile>, context: CommonBackendContext, input: IrFile): IrFile {
            input.acceptVoid(PatchDeclarationParentsVisitor())
            return input
        }
    },
    name = "PatchParents$number",
    description = "Patch parent references in IrFile, pass $number",
    nlevels = 0
)

private val arrayConstructorPhase = makeIrFilePhase(
    ::ArrayConstructorLowering,
    name = "ArrayConstructor",
    description = "Transform `Array(size) { index -> value }` into a loop"
)

private val expectDeclarationsRemovingPhase = makeIrFilePhase(
    ::ExpectDeclarationsRemoving,
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

private val defaultArgumentStubPhase = makeIrFilePhase<CommonBackendContext>(
    { context -> DefaultArgumentStubGenerator(context, false) },
    name = "DefaultArgumentsStubGenerator",
    description = "Generate synthetic stubs for functions with default parameter values"
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

private val innerClassesPhase = makeIrFilePhase(
    ::InnerClassesLowering,
    name = "InnerClasses",
    description = "Add 'outer this' fields to inner classes",
    prerequisite = setOf(localDeclarationsPhase)
)

val jvmPhases = namedIrFilePhase<JvmBackendContext>(
    name = "IrLowering",
    description = "IR lowering",
    lower = expectDeclarationsRemovingPhase then
            fileClassPhase then
            kCallableNamePropertyPhase then
            arrayConstructorPhase then

            lateinitPhase then

            moveOrCopyCompanionObjectFieldsPhase then
            propertyReferencePhase then
            constPhase then
            propertiesToFieldsPhase then
            propertiesPhase then
            renameFieldsPhase then
            annotationPhase then
            tailrecPhase then

            jvmDefaultConstructorPhase then
            defaultArgumentStubPhase then

            interfacePhase then
            interfaceDelegationPhase then
            interfaceSuperCallsPhase then
            sharedVariablesPhase then

            makePatchParentsPhase(1) then

            enumWhenPhase then
            singletonReferencesPhase then
            localDeclarationsPhase then
            singleAbstractMethodPhase then
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
            jvmStaticAnnotationPhase then
            staticDefaultFunctionPhase then

            toArrayPhase then
            flattenStringConcatenationPhase then
            foldConstantLoweringPhase then
            computeStringTrimPhase then
            jvmBuiltinOptimizationLoweringPhase then
            additionalClassAnnotationPhase then

            // should be last transformation
            removeDeclarationsThatWouldBeInlined then
            makePatchParentsPhase(3)
)

class JvmLower(val context: JvmBackendContext) {
    fun lower(irFile: IrFile) {
        // TODO run lowering passes as callbacks in bottom-up visitor
        jvmPhases.invokeToplevel(context.phaseConfig, context, irFile)
    }
}
