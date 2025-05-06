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
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.descriptors.toIrBasedKotlinType
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentDeclarationsWithSelf
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

/**
 * Moves local declarations to classes.
 */
@PhaseDescription(
    name = "JvmLocalDeclarations",
    prerequisite = [FunctionReferenceLowering::class, SharedVariablesLowering::class]
)
internal class JvmLocalDeclarationsLowering(override val context: JvmBackendContext) : LocalDeclarationsLowering(
    context,
    NameUtils::sanitizeAsJavaIdentifier,
    JvmVisibilityPolicy,
    compatibilityModeForInlinedLocalDelegatedPropertyAccessors = true,
    forceFieldsForInlineCaptures = true,
    remapTypesInExtractedLocalDeclarations = false,
    allConstructorsWithCapturedConstructorCreated = context.allConstructorsWithCapturedConstructorCreated,
    closureBuilders = context.evaluatorData?.localDeclarationsData?.closureBuilders ?: mutableMapOf(),
    transformedDeclarations = context.evaluatorData?.localDeclarationsData?.transformedDeclarations ?: mutableMapOf(),
    newParameterToCaptured = context.evaluatorData?.localDeclarationsData?.newParameterToCaptured ?: mutableMapOf(),
    newParameterToOld = context.evaluatorData?.localDeclarationsData?.newParameterToOld ?: mutableMapOf(),
    oldParameterToNew = context.evaluatorData?.localDeclarationsData?.oldParameterToNew ?: mutableMapOf(),
) {
    override fun getReplacementSymbolForCaptured(container: IrDeclaration, symbol: IrValueSymbol): IrValueSymbol {
        if (context.evaluatorData?.evaluatorGeneratedFunction == container && !symbol.owner.parentDeclarationsWithSelf.contains(container)) {
            val newParameter = (container as IrFunction).addValueParameter {
                type = symbol.owner.type
                name = symbol.owner.name
            }
            context.state.newFragmentCaptureParameters.add(
                Triple(
                    symbol.owner.name.asString(),
                    symbol.owner.type.toIrBasedKotlinType(),
                    symbol.owner.toIrBasedDescriptor()
                )
            )
            return newParameter.symbol
        }
        return symbol
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
