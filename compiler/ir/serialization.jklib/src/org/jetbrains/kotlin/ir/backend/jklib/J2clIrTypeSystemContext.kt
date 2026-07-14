/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jklib

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

class J2clIrTypeSystemContext(private val delegate: IrTypeSystemContext) : IrTypeSystemContext by delegate {
    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        if (delegate.areEqualTypeConstructors(c1, c2)) return true

        if (c1 is IrClassSymbol && c2 is IrClassSymbol) {
            if (c1.isBound && c2.isBound) {
                val fq1 = c1.owner.classId?.asSingleFqName()
                val fq2 = c2.owner.classId?.asSingleFqName()
                if (fq1 != null && fq2 != null) {
                    val mapped1 = JavaToKotlinClassMap.mapKotlinToJava(fq1.toUnsafe())?.asSingleFqName() ?: fq1
                    val mapped2 = JavaToKotlinClassMap.mapKotlinToJava(fq2.toUnsafe())?.asSingleFqName() ?: fq2
                    return mapped1 == mapped2
                }
            }
        }
        return false
    }
}
