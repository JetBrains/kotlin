/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter

class MutableJavaTypeParameterStack : JavaTypeParameterStack() {
    private val typeParameterMap = mutableMapOf<JavaTypeParameter, FirTypeParameterSymbol>()

    /**
     * The [FirRegularClassSymbol] of the [org.jetbrains.kotlin.fir.java.declarations.FirJavaClass]
     * this stack belongs to. Set by [org.jetbrains.kotlin.fir.java.FirJavaFacade.convertJavaClassToFir].
     *
     * Used by `JavaTypeConversion.findOuterTypeArgsFromHierarchy` to walk the
     * lexical containing-class chain at the type-reference site without requiring the model
     * to expose `JavaClassifierType.containingClassIds`. [addStack] does not propagate this
     * field — each [MutableJavaTypeParameterStack] owns its own containing-class identity.
     */
    var containingClassSymbol: FirRegularClassSymbol? = null

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
        it.containingClassSymbol = containingClassSymbol
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
