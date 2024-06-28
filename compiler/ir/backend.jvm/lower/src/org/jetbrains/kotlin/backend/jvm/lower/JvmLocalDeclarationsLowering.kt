/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

@PhaseDescription(
    name = "JvmLocalDeclarations",
    description = "Move local declarations to classes",
    prerequisite = [FunctionReferenceLowering::class, SharedVariablesLowering::class]
)
internal class JvmLocalDeclarationsLowering(context: JvmBackendContext) : LocalDeclarationsLowering(
    context,
    NameUtils::sanitizeAsJavaIdentifier,
    JvmVisibilityPolicy,
    compatibilityModeForInlinedLocalDelegatedPropertyAccessors = true,
    forceFieldsForInlineCaptures = true,
) {
    override fun postLocalDeclarationLoweringCallback(
        localFunctions: Map<IrFunction, LocalFunctionContext>,
        newParameterToOld: Map<IrValueParameter, IrValueParameter>,
        newParameterToCaptured: Map<IrValueParameter, IrValueSymbol>,
    ) {
        val data = (context as JvmBackendContext).evaluatorData?.localDeclarationsLoweringData ?: return
        for ((localFunction, localContext) in localFunctions) {
            data[localFunction] = JvmBackendContext.LocalFunctionData(localContext, newParameterToOld, newParameterToCaptured)
        }
    }

    override fun IrClass.getConstructorsThatCouldCaptureParamsWithoutFieldCreating(): Iterable<IrConstructor> =
        declarations.filterIsInstanceAnd<IrConstructor> {
            it.delegationKind(context.irBuiltIns) == ConstructorDelegationKind.CALLS_SUPER
        }
}

internal val IrClass.isGeneratedLambdaClass: Boolean
    get() = origin == JvmLoweredDeclarationOrigin.LAMBDA_IMPL ||
            origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA ||
            origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL ||
            origin == JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE

internal object JvmVisibilityPolicy : VisibilityPolicy {
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
}
