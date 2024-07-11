/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.classes.hasTypeForValueClassInSignature

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun String.isSuppressedFinalModifier(containingClass: SymbolLightClassBase, symbol: KaCallableSymbol): Boolean {
    return this == PsiModifier.FINAL && (containingClass.isEnum && symbol.origin == KaSymbolOrigin.SOURCE_MEMBER_GENERATED || containingClass.isInterface)
}

/**
 * Symbol light classes are not materializing declarations with value classes in signatures, because mostly
 * they have mangled names which cannot be called from Java, so we don't need them.
 * And such declarations are filtered out by [hasTypeForValueClassInSignature].
 *
 * So there is no need trying to unwrap classes in signatures of such declarations.
 * But we still have a few places there we have valid Java names, so we have to unwrap value classes if:
 * - They are in the return type position of top-level callable. Such declarations don't have a mangled name.
 * - Backing fields. They don't have mangled names as each class may have only one filed with the same name.
 * - Declarations with [JvmName] annotation. This annotation overrides the mangled name, so such declarations
 * are accessible from Java.
 *
 * @return **true** if [this] method may have a value class in signature
 *
 * @see hasTypeForValueClassInSignature
 */
internal fun SymbolLightMethodBase.canHaveValueClassInSignature(): Boolean = when (this) {
    is SymbolLightSimpleMethod -> canHaveValueClassInSignature()
    is SymbolLightAccessorMethod -> canHaveValueClassInSignature()
    else -> false
}
