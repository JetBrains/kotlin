/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.types.model.*

open class IrTypeCheckerContext(override val irBuiltIns: IrBuiltIns) : IrTypeSystemContext, AbstractTypeCheckerContext() {

    override fun substitutionSupertypePolicy(type: SimpleTypeMarker): SupertypesPolicy.DoCustomTransform {
        require(type is IrSimpleType)
        val parameters = extractTypeParameters((type.classifier as IrClassSymbol).owner).map { it.symbol }
        val typeSubstitutor = IrTypeSubstitutor(parameters, type.arguments, irBuiltIns)

        return object : SupertypesPolicy.DoCustomTransform() {
            override fun transformType(context: AbstractTypeCheckerContext, type: KotlinTypeMarker): SimpleTypeMarker {
                require(type is IrType)
                return typeSubstitutor.substitute(type) as IrSimpleType
            }
        }
    }

    override val isErrorTypeEqualsToAnything get() = false
    override val isStubTypeEqualsToAnything get() = false

    override val KotlinTypeMarker.isAllowedTypeVariable: Boolean
        get() = false

    override fun newBaseTypeCheckerContext(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): AbstractTypeCheckerContext = IrTypeCheckerContext(irBuiltIns)

    override fun KotlinTypeMarker.isUninferredParameter(): Boolean = false
    override fun KotlinTypeMarker.withNullability(nullable: Boolean): KotlinTypeMarker {
        if (this.isSimpleType()) {
            return this.asSimpleType()!!.withNullability(nullable)
        } else {
            error("withNullability for non-simple types is not supported in IR")
        }
    }

    override fun captureFromExpression(type: KotlinTypeMarker): KotlinTypeMarker? =
        error("Captured type is unsupported in IR")

    override fun DefinitelyNotNullTypeMarker.original(): SimpleTypeMarker =
        error("DefinitelyNotNull type is unsupported in IR")

    override fun KotlinTypeMarker.makeDefinitelyNotNullOrNotNull(): KotlinTypeMarker {
        error("makeDefinitelyNotNullOrNotNull is not supported in IR")
    }

    override fun SimpleTypeMarker.makeSimpleTypeDefinitelyNotNullOrNotNull(): SimpleTypeMarker {
        error("makeSimpleTypeDefinitelyNotNullOrNotNull is not yet supported in IR")
    }
}
