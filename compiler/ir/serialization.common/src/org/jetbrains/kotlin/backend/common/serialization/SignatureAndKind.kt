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

    /**
     * Return the signature and the kind representing the top-level declaration for the declaration represented by
     * the current instance of [SignatureAndKind]:
     * - For a top-level declaration: the current instance itself.
     * - For an accessor or a backing field of a top-level property: the property.
     * - For a declaration nested into a class: the top-level class.
     * - For a local declaration, such as a type parameter, value parameter or a declaration located inside a body: `null`.
     */
    fun topLevelOrNull(): SignatureAndKind? {
        val propertyOrSelf = correspondingPropertyOrNull() ?: this
        if (propertyOrSelf.signature.isLocal) return null

        val topLevelSignature = propertyOrSelf.signature.topLevelSignature()

        return if (topLevelSignature == propertyOrSelf.signature)
            propertyOrSelf
        else
            SignatureAndKind(topLevelSignature, CLASS_SYMBOL)
    }

    /**
     * Return the signature and the kind representing the corresponding property if the current instance of [SignatureAndKind]
     * represents one of the following:
     * - An accessor.
     * - A backing field, or a (standalone) field of a delegated property or an accessor-less property.
     *
     * Otherwise, return `null`.
     */
    fun correspondingPropertyOrNull(): SignatureAndKind? {
        val propertySignature = when (kind) {
            FUNCTION_SYMBOL -> signature.propertyOfAccessorOrNull()
            FIELD_SYMBOL, STANDALONE_FIELD_SYMBOL -> signature.propertyOfFieldOrNull()
            else -> null
        } ?: return null

        return SignatureAndKind(propertySignature, PROPERTY_SYMBOL)
    }

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

private fun IdSignature.propertyOfAccessorOrNull(): IdSignature? = when (this) {
    is IdSignature.AccessorSignature -> propertySignature
    is IdSignature.CompositeSignature if container is IdSignature.FileSignature ->
        inner.propertyOfAccessorOrNull()?.let { IdSignature.CompositeSignature(container, it) }
    else -> null
}

private fun IdSignature.propertyOfFieldOrNull(): IdSignature? = when (this) {
    is IdSignature.CompositeSignature if inner is IdSignature.LocalSignature -> container
    else -> null
}
