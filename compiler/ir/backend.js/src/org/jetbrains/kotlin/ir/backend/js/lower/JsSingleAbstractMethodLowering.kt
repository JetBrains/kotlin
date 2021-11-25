/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.lower.SingleAbstractMethodLowering
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.parentClassOrNull

class JsSingleAbstractMethodLowering(context: CommonBackendContext) : SingleAbstractMethodLowering(context), BodyLoweringPass {

    override fun getWrapperVisibility(expression: IrTypeOperatorCall, scopes: List<ScopeWithIr>): DescriptorVisibility =
        DescriptorVisibilities.LOCAL

    override val IrType.needEqualsHashCodeMethods get() = false

    private var enclosingBodyContainer: IrDeclaration? = null

    override fun lower(irFile: IrFile) {
        super<BodyLoweringPass>.lower(irFile)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        cachedImplementations.clear()
        inlineCachedImplementations.clear()
        enclosingContainer = container.parentClassOrNull ?: container.file
        enclosingBodyContainer = container

        irBody.transformChildrenVoid()

        for (wrapper in cachedImplementations.values + inlineCachedImplementations.values) {
            val parentClass = wrapper.parent as IrDeclarationContainer
            context.irFactory.stageController.unrestrictDeclarationListsAccess {
                parentClass.declarations += wrapper
            }
        }
    }

    override fun currentScopeSymbol(): IrSymbol? {
        return super.currentScopeSymbol() ?: (enclosingBodyContainer as? IrSymbolOwner)?.symbol
    }

    override fun getSuperTypeForWrapper(typeOperand: IrType): IrType {
        // FE doesn't allow type parameters for now.
        // And since there is a to-do in common SingleAbstractMethodLowering (at function visitTypeOperator),
        // we don't have to be more saint than a pope here.
        return typeOperand.classOrNull?.defaultType
            ?: compilationException("Unsupported SAM conversion", typeOperand)
    }
}
