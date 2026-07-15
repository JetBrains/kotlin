/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.unsigned

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.UnsignedType
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.ExpectActualModifier
import unsigned.types.BaseUnsignedTypeGenerator
import java.io.PrintWriter

class WasmUnsignedTypeGenerator(type: UnsignedType, out: PrintWriter) : BaseUnsignedTypeGenerator(type, out, ExpectActualModifier.Actual) {
    override val extraImports = setOf("kotlin.wasm.internal.*")

    override fun floatingConversionBody(otherType: PrimitiveType): String = when (type) {
        UnsignedType.UINT, UnsignedType.ULONG ->
            "${type.capitalized.lowercase()}To${otherType.capitalized}(this.data)"
        else -> super.floatingConversionBody(otherType)
    }

    override fun fromFloatingPointBody(otherType: PrimitiveType): String = when (type) {
        UnsignedType.UINT, UnsignedType.ULONG ->
            "${otherType.capitalized.lowercase()}To${type.capitalized}(this)"
        else -> super.fromFloatingPointBody(otherType)
    }

    override fun signedConversionBody(otherType: UnsignedType): String = when (type) {
        UnsignedType.UINT if otherType == UnsignedType.ULONG -> "uintToLong(this.data)"
        else -> super.signedConversionBody(otherType)
    }

    override fun unsignedConversionBody(otherType: UnsignedType): String = when (type) {
        UnsignedType.UINT if otherType == UnsignedType.ULONG -> "uintToULong(this.data)"
        else -> super.unsignedConversionBody(otherType)
    }
}
