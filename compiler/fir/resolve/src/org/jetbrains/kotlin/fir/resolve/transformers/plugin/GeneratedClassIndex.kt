/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import com.google.common.collect.ArrayListMultimap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

data class GeneratedClass(val klass: FirRegularClass, val owner: FirAnnotatedDeclaration)

abstract class GeneratedClassIndex : FirSessionComponent {
    companion object {
        fun create(): GeneratedClassIndex {
            return GeneratedClassIndexImpl()
        }
    }

    abstract fun registerClass(klass: FirRegularClass, owner: FirAnnotatedDeclaration)
    abstract operator fun get(key: FirPluginKey): List<GeneratedClass>
}

val FirSession.generatedClassIndex: GeneratedClassIndex by FirSession.sessionComponentAccessor()

private class GeneratedClassIndexImpl : GeneratedClassIndex() {
    private val index: ArrayListMultimap<FirPluginKey, GeneratedClass> = ArrayListMultimap.create()

    override fun registerClass(klass: FirRegularClass, owner: FirAnnotatedDeclaration) {
        val key = (klass.origin as FirDeclarationOrigin.Plugin).key
        index.put(key, GeneratedClass(klass, owner))
    }

    override fun get(key: FirPluginKey): List<GeneratedClass> {
        return index.get(key)
    }
}