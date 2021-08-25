/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.types.AbstractTypePreparator
import org.jetbrains.kotlin.types.AbstractTypeRefiner
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

open class IrTypeCheckerState(override val typeSystemContext: IrTypeSystemContext): TypeCheckerState() {

    override val kotlinTypePreparator: AbstractTypePreparator
        get() = AbstractTypePreparator.Default
    override val kotlinTypeRefiner: AbstractTypeRefiner
        get() = AbstractTypeRefiner.Default

    val irBuiltIns: IrBuiltIns get() = typeSystemContext.irBuiltIns

    override fun substitutionSupertypePolicy(type: SimpleTypeMarker): SupertypesPolicy.DoCustomTransform {
        require(type is IrSimpleType)
        val parameters = extractTypeParameters((type.classifier as IrClassSymbol).owner).map { it.symbol }
        val typeSubstitutor = IrTypeSubstitutor(parameters, type.arguments, irBuiltIns)

        return object : SupertypesPolicy.DoCustomTransform() {
            override fun transformType(state: TypeCheckerState, type: KotlinTypeMarker): SimpleTypeMarker {
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
