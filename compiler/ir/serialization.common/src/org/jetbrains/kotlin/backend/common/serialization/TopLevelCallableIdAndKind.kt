/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData.SymbolKind
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.isClassSignature
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * A key for looking up top-level callables (functions or properties) in a module by their [CallableId].
 *
 * A [CallableId] alone does not identify a single declaration: there can be several overloaded functions
 * with the same [CallableId], and a function and a property can share the same [CallableId] too.
 * So [kind] is used to tell functions from properties, and a single [TopLevelCallableIdAndKind]
 * may still correspond to multiple [IdSignature]s (one per overload).
 *
 * @property callableId The package FQ name and the name of the top-level callable.
 * @property kind The kind of the callable. Only [BinarySymbolData.SymbolKind.FUNCTION_SYMBOL] and
 *   [BinarySymbolData.SymbolKind.PROPERTY_SYMBOL] are allowed.
 */
data class TopLevelCallableIdAndKind(val callableId: CallableId, val kind: SymbolKind) {
    val packageFqName: FqName get() = callableId.packageName

    constructor(signature: IdSignature.CommonSignature, kind: SymbolKind) : this(
        callableId = signature.checkSignature().toCallableId(),
        kind = kind,
    )

    constructor(symbol: IrSymbol) : this(
        callableId = symbol.getCallableSignatureOrFail().toCallableId(),
        kind = symbol.kind(),
    )

    init {
        require(callableId.className == null && callableId.classId == null) {
            "Can not create ${TopLevelCallableIdAndKind::class.java.simpleName} for a class member: $kind, $callableId"
        }

        require(kind == FUNCTION_SYMBOL || kind == PROPERTY_SYMBOL) {
            "Can not create ${TopLevelCallableIdAndKind::class.java.simpleName} for unsupported symbol kind: $kind, $callableId"
        }
    }

    override fun toString() = "$callableId, kind=$kind"
}

private fun IrSymbol.getCallableSignatureOrFail(): IdSignature.CommonSignature {
    when (val signature = signature) {
        is IdSignature.CommonSignature -> return signature.checkSignature()
        null -> error("Can not create ${TopLevelCallableIdAndKind::class.java.simpleName}, no signature in symbol $this")
        else -> error("Can not create ${TopLevelCallableIdAndKind::class.java.simpleName} for symbol $this, unexpected signature type: ${signature::class.java.simpleName}, $signature")
    }
}

private fun IdSignature.CommonSignature.checkSignature(): IdSignature.CommonSignature {
    check(!isClassSignature()) { "Can not create ${TopLevelCallableIdAndKind::class.java.simpleName} for class signature $this" }
    check(nameSegments.size == 1) { "Can not create ${TopLevelCallableIdAndKind::class.java.simpleName} for a non-top-level declaration signature $this" }
    return this
}

private fun IdSignature.CommonSignature.toCallableId(): CallableId =
    CallableId(packageFqName(), Name.identifier(declarationFqName))
