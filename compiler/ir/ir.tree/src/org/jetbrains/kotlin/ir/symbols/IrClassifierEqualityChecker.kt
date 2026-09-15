/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.symbols

import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.hasEqualClassId
import org.jetbrains.kotlin.ir.util.isClassSignature
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

interface IrClassifierEqualityChecker {
    fun areEqual(left: IrClassifierSymbol, right: IrClassifierSymbol): Boolean

    fun getHashCode(symbol: IrClassifierSymbol): Int
}

object FqNameEqualityChecker : IrClassifierEqualityChecker {
    override fun areEqual(left: IrClassifierSymbol, right: IrClassifierSymbol): Boolean {
        return when {
            left === right -> true
            left.isBound == right.isBound -> left.signature != null && left.signature == right.signature
            left !is IrClassSymbol || right !is IrClassSymbol -> false
            !left.isBound && right.isBound -> right.owner.hasEqualClassId(left.signature?.classId() ?: return false)
            left.isBound && !right.isBound -> left.owner.hasEqualClassId(right.signature?.classId() ?: return false)
            else -> false
        }
    }

    override fun getHashCode(symbol: IrClassifierSymbol): Int =
        symbol.signature?.hashCode() ?: symbol.hashCode()

    private fun IdSignature.classId(): ClassId? {
        if (this !is IdSignature.CommonSignature || !this.isClassSignature()) return null

        if (nameSegments.isEmpty()) return null
        return ClassId(packageFqName(), FqName.fromSegments(nameSegments), isLocal = false)
    }
}
