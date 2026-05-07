/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter

class MutableJavaTypeParameterStack : JavaTypeParameterStack() {
    private val typeParameterMap = mutableMapOf<JavaTypeParameter, FirTypeParameterSymbol>()

    fun addParameter(javaTypeParameter: JavaTypeParameter, symbol: FirTypeParameterSymbol) {
        typeParameterMap[javaTypeParameter] = symbol
    }

    fun addStack(javaTypeParameterStack: MutableJavaTypeParameterStack) {
        typeParameterMap += javaTypeParameterStack.typeParameterMap
    }

    override operator fun get(javaTypeParameter: JavaTypeParameter): FirTypeParameterSymbol? {
        return typeParameterMap[javaTypeParameter]
    }

    override operator fun iterator(): Iterator<Map.Entry<JavaTypeParameter, FirTypeParameterSymbol>> {
        return typeParameterMap.iterator()
    }

    fun copy(): MutableJavaTypeParameterStack = MutableJavaTypeParameterStack().also {
        it.typeParameterMap += typeParameterMap
    }
}

abstract class JavaTypeParameterStack : Iterable<Map.Entry<JavaTypeParameter, FirTypeParameterSymbol>> {
    abstract operator fun get(javaTypeParameter: JavaTypeParameter): FirTypeParameterSymbol?

    companion object {
        val EMPTY: JavaTypeParameterStack = object : JavaTypeParameterStack() {
            override fun get(javaTypeParameter: JavaTypeParameter): FirTypeParameterSymbol? = null
            override fun iterator(): Iterator<Map.Entry<JavaTypeParameter, FirTypeParameterSymbol>> {
                return emptyMap<JavaTypeParameter, FirTypeParameterSymbol>().iterator()
            }
        }
    }
}

/**
 * A [JavaTypeParameter] that directly carries its [FirTypeParameterSymbol], bypassing the
 * [MutableJavaTypeParameterStack] lookup that PSI- / binary- / source-`java-direct`-backed
 * `JavaTypeParameter` impls rely on.
 *
 * Used by the Java Model's `FirBackedJavaClassAdapter` (Step 4.5c per
 * `compiler/java-direct/implDocs/INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md`): the adapter
 * synthesises `JavaTypeParameter` instances on demand for cross-file references and they are
 * not — and cannot be — registered in any per-`FirJavaClass` stack populated at
 * `FirJavaFacade.convertJavaClassToFir` time. Carrying the symbol on the wrapper itself lets
 * `JavaTypeConversion`'s `is JavaTypeParameter ->` branch resolve it directly.
 *
 * The pre-existing stack lookup remains the path for PSI / binary / source-`java-direct`
 * `JavaTypeParameter` impls; this interface is checked **first** as a fast path before the
 * stack lookup.
 */
interface JavaTypeParameterWithFirSymbol : JavaTypeParameter {
    val firTypeParameterSymbol: FirTypeParameterSymbol
}
