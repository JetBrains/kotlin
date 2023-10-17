/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types

import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirClassifierSymbol
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
    typeParameter: BirTypeParameter
) : BirSimpleType(null), CapturedTypeMarker {

    override val variance: Variance
        get() = TODO("Not yet implemented")

    val constructor: Constructor = Constructor(projection, typeParameter)

    override val classifier: BirClassifierSymbol get() = error("Captured Type does not have a classifier")
    override val arguments: List<BirTypeArgument> get() = emptyList()
    override val abbreviation: BirTypeAbbreviation? get() = null
    override val nullability: SimpleTypeNullability get() = SimpleTypeNullability.DEFINITELY_NOT_NULL
    override val annotations: List<BirConstructorCall> get() = emptyList()

    override fun equals(other: Any?): Boolean {
        return other is BirCapturedType
                && captureStatus == other.captureStatus
                && lowerType == other.lowerType
                && constructor === other.constructor
    }

    override fun hashCode(): Int {
        return (captureStatus.hashCode() * 31 + (lowerType?.hashCode() ?: 0)) * 31 + constructor.hashCode()
    }

    data class Constructor(val argument: BirTypeArgument, val typeParameter: BirTypeParameter) : CapturedTypeConstructorMarker {
        private var _superTypes: List<BirType> = emptyList()

        val superTypes: List<BirType> get() = _superTypes

        fun initSuperTypes(superTypes: List<BirType>) {
            _superTypes = superTypes
        }
    }
}
