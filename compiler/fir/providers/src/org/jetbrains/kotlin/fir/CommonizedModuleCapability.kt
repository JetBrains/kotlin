/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.forEachType
import kotlin.reflect.KClass

object CommonizedModuleCapability : FirModuleCapability() {
    override val key: KClass<out FirModuleCapability> = CommonizedModuleCapability::class
}

val FirBasedSymbol<*>.isMarkedAsCommonizedModule: Boolean
    get() =
        fir.moduleData.capabilities.contains(CommonizedModuleCapability)

context(sessionHolder: SessionHolder)
val ConeKotlinType.isMarkedAsCommonizedModule: Boolean
    get() = anyExpansionOfAnyType { symbol -> symbol.isMarkedAsCommonizedModule }

context(sessionHolder: SessionHolder)
inline fun ConeKotlinType.anyExpansionOfAnyType(block: (FirClassifierSymbol<*>) -> Boolean): Boolean {
    forEachType { type ->
        type.forEachExpansion { symbol ->
            if (block(symbol)) {
                return true
            }
        }
    }

    return false
}

context(sessionHolder: SessionHolder)
inline fun ConeKotlinType.forEachExpansion(block: (FirClassifierSymbol<*>) -> Unit) {
    var symbol = this.toSymbol()

    while (symbol is FirTypeAliasSymbol) {
        block(symbol)
        symbol = symbol.resolvedExpandedTypeRef.coneType.toSymbol()
    }

    if (symbol != null) {
        block(symbol)
    }
}
