/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.unsigned

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.UnsignedType
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.ExpectActualModifier
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.MethodBuilder
import unsigned.types.BaseUnsignedTypeGenerator
import java.io.PrintWriter

class WasmUnsignedTypeGenerator(type: UnsignedType, out: PrintWriter) : BaseUnsignedTypeGenerator(type, out, ExpectActualModifier.Actual) {
    override val extraImports = setOf("kotlin.wasm.internal.*")

    override fun compareToBody(otherType: UnsignedType): String = when (type) {
        otherType if otherType == UnsignedType.UINT -> "wasm_u32_compareTo(this.data, other.data)"
        otherType if otherType == UnsignedType.ULONG -> "wasm_u64_compareTo(this.data, other.data)"
        else -> super.compareToBody(otherType)
    }

    override fun binaryOperatorsBody(operator: String, otherType: UnsignedType, returnType: UnsignedType): String {
        val isBinaryOperationOnSimilarTypes = type == otherType && type == returnType
        return when (operator) {
            "rem" if isBinaryOperationOnSimilarTypes -> "${type.capitalized}(wasm_i${type.bitSize}_rem_u(this.data, other.data))"
            "div" if isBinaryOperationOnSimilarTypes -> "${type.capitalized}(wasm_i${type.bitSize}_div_u(this.data, other.data))"
            else -> super.binaryOperatorsBody(operator, otherType, returnType)
        }
    }

    override fun floatingConversionBody(otherType: PrimitiveType): String = when (type) {
        UnsignedType.UINT, UnsignedType.ULONG -> "wasm_f${otherType.bitSize}_convert_i${type.bitSize}_u(this.data)"
        else -> super.floatingConversionBody(otherType)
    }

    override fun fromFloatingPointBody(otherType: PrimitiveType): String = when (type) {
        UnsignedType.UINT, UnsignedType.ULONG -> "${type.capitalized}(wasm_i${type.bitSize}_trunc_sat_f${otherType.bitSize}_u(this))"
        else -> super.fromFloatingPointBody(otherType)
    }

    override fun signedConversionBody(otherType: UnsignedType): String = when (type) {
        UnsignedType.UINT if otherType == UnsignedType.ULONG -> "wasm_i64_extend_i32_u(this.data)"
        else -> super.signedConversionBody(otherType)
    }

    override fun unsignedConversionBody(otherType: UnsignedType): String = when (type) {
        UnsignedType.UINT if otherType == UnsignedType.ULONG -> "${otherType.capitalized}(wasm_i64_extend_i32_u(this.data))"
        else -> super.unsignedConversionBody(otherType)
    }

    override fun toStringHashCodeBody(): String = when (type) {
        UnsignedType.UINT, UnsignedType.ULONG -> "utoa${type.bitSize}(this)"
        else -> super.toStringHashCodeBody()
    }
}
