/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParameterBuilder
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter

internal class JavaTypeParameterStack {

    private val typeParameterMap = mutableMapOf<JavaTypeParameter, FirTypeParameterBuilder>()

    fun add(javaTypeParameter: JavaTypeParameter, firTypeParameterBuilder: FirTypeParameterBuilder) {
        typeParameterMap[javaTypeParameter] = firTypeParameterBuilder
    }

    fun addStack(javaTypeParameterStack: JavaTypeParameterStack) {
        typeParameterMap += javaTypeParameterStack.typeParameterMap
    }

    fun remove(javaTypeParameter: JavaTypeParameter) {
        typeParameterMap.remove(javaTypeParameter)
    }

    operator fun get(javaTypeParameter: JavaTypeParameter): FirTypeParameterSymbol {
        return safeGet(javaTypeParameter)
            ?: throw IllegalArgumentException("Cannot find Java type parameter $javaTypeParameter in stack")
    }

    fun safeGet(javaTypeParameter: JavaTypeParameter) = typeParameterMap[javaTypeParameter]?.symbol

    fun getBuilder(javaTypeParameter: JavaTypeParameter) = typeParameterMap[javaTypeParameter]

    companion object {
        val EMPTY: JavaTypeParameterStack = JavaTypeParameterStack()
    }
}