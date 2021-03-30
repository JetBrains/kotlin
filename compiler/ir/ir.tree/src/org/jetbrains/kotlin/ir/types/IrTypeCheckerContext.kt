/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.types.model.*

open class IrTypeCheckerContext(override val typeSystemContext: IrTypeSystemContext): AbstractTypeCheckerContext() {

    val irBuiltIns: IrBuiltIns get() = typeSystemContext.irBuiltIns

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
}
