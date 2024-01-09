/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types.utils

import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.getPublicSignature
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

fun BirType.isPrimitiveType(nullable: Boolean = false): Boolean =
    nullable == isMarkedNullable() && getPrimitiveType() != null

fun BirType.isNullablePrimitiveType(): Boolean = isPrimitiveType(true)

fun BirType.getPrimitiveType(): PrimitiveType? =
    getPrimitiveOrUnsignedType(idSignatureToPrimitiveType, shortNameToPrimitiveType)

fun BirType.isUnsignedType(nullable: Boolean = false): Boolean =
    nullable == isMarkedNullable() && getUnsignedType() != null

fun BirType.getUnsignedType(): UnsignedType? =
    getPrimitiveOrUnsignedType(idSignatureToUnsignedType, shortNameToUnsignedType)

private fun <T : Enum<T>> BirType.getPrimitiveOrUnsignedType(
    byIdSignature: Map<IdSignature.CommonSignature, T>,
    byShortName: Map<Name, T>
): T? {
    if (this !is BirSimpleType) return null
    val symbol = classifier as? IrClassSymbol ?: return null
    if (symbol.signature != null) return byIdSignature[symbol.signature]

    val klass = symbol.owner
    val parent = klass.parent
    if (parent !is IrPackageFragment || parent.packageFqName != StandardNames.BUILT_INS_PACKAGE_FQ_NAME) return null
    return byShortName[klass.name]
}

private val idSignatureToPrimitiveType: Map<IdSignature.CommonSignature, PrimitiveType> =
    PrimitiveType.values().associateBy {
        getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, it.typeName.asString())
    }

private val shortNameToPrimitiveType: Map<Name, PrimitiveType> =
    PrimitiveType.values().associateBy(PrimitiveType::typeName)

private val idSignatureToUnsignedType: Map<IdSignature.CommonSignature, UnsignedType> =
    UnsignedType.values().associateBy {
        getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, it.typeName.asString())
    }

private val shortNameToUnsignedType: Map<Name, UnsignedType> =
    UnsignedType.values().associateBy(UnsignedType::typeName)

val primitiveArrayTypesSignatures: Map<PrimitiveType, IdSignature.CommonSignature> =
    PrimitiveType.values().associateWith {
        getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, "${it.typeName.asString()}Array")
    }


