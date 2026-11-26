/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData.SymbolKind
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName

/**
 * A key for looking up a declaration (or its symbol) in the linker: the [signature] tells which declaration
 * it is, and the [kind] tells what kind of declaration (and so what kind of [IrSymbol]) is expected.
 */
data class SignatureAndKind(val signature: IdSignature, val kind: SymbolKind) {
    val packageFqName: FqName get() = signature.packageFqName()

    override fun toString() = "${signature.render()}, kind=$kind"
}

/** Creates a [SignatureAndKind] from this signature and the given symbol [kind]. */
infix fun IdSignature.and(kind: SymbolKind) = SignatureAndKind(this, kind)

/** The [SignatureAndKind] of this symbol, or `null` if the symbol has no signature. */
val IrSymbol.signatureAndKindOrNull: SignatureAndKind?
    get() = signature?.and(kind())

/** The [SignatureAndKind] of this symbol; fails if the symbol has no signature. */
val IrSymbol.signatureAndKind: SignatureAndKind
    get() = signatureAndKindOrNull
        ?: error("Can not create ${SignatureAndKind::class.java.simpleName} for a symbol without a signature: $this")
