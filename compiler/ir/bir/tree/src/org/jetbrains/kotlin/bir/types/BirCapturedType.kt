/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types

import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirClassifierSymbol
import org.jetbrains.kotlin.bir.util.render
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.types.model.CapturedTypeConstructorMarker
import org.jetbrains.kotlin.types.model.CapturedTypeMarker

// Please note this type is not denotable which means it could only exist inside type system
class BirCapturedType(
    val captureStatus: CaptureStatus,
    val lowerType: BirType?,
    projection: BirTypeArgument,
    typeParameter: BirTypeParameter,
    override val nullability: SimpleTypeNullability,
    override val annotations: List<BirConstructorCall>,
    override val abbreviation: BirTypeAbbreviation?,
) : BirSimpleType(null), CapturedTypeMarker {
    val constructor: Constructor = Constructor(projection, typeParameter)

    override val classifier: BirClassifierSymbol get() = error("Captured Type does not have a classifier")
    override val arguments: List<BirTypeArgument> get() = emptyList()

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)

    override fun toString(): String = "IrCapturedType(${constructor.argument.render()}"

    data class Constructor(val argument: BirTypeArgument, val typeParameter: BirTypeParameter) : CapturedTypeConstructorMarker {
        var superTypes: List<BirType> = emptyList()
            private set

        fun initSuperTypes(superTypes: List<BirType>) {
            this.superTypes = superTypes
        }
    }
}
