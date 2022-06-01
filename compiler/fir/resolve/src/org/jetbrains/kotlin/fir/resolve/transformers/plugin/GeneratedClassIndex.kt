/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import com.google.common.collect.ArrayListMultimap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

data class GeneratedClass(val klass: FirRegularClass, val owner: FirDeclaration)

abstract class GeneratedClassIndex : FirSessionComponent {
    companion object {
        fun create(): GeneratedClassIndex {
            return GeneratedClassIndexImpl()
        }
    }

    abstract fun registerClass(klass: FirRegularClass, owner: FirDeclaration)
    abstract operator fun get(key: GeneratedDeclarationKey): List<GeneratedClass>
}

val FirSession.generatedClassIndex: GeneratedClassIndex by FirSession.sessionComponentAccessor()

@ThreadSafeMutableState
private class GeneratedClassIndexImpl : GeneratedClassIndex() {
    private val index: ArrayListMultimap<GeneratedDeclarationKey, GeneratedClass> = ArrayListMultimap.create()

    override fun registerClass(klass: FirRegularClass, owner: FirDeclaration) {
        val key = (klass.origin as FirDeclarationOrigin.Plugin).key
        index.put(key, GeneratedClass(klass, owner))
    }

    override fun get(key: GeneratedDeclarationKey): List<GeneratedClass> {
        return index.get(key)
    }
}
