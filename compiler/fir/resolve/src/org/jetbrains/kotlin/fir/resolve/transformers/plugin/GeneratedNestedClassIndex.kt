/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import com.google.common.collect.ArrayListMultimap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

data class GeneratedNestedClass(val nestedClass: FirRegularClass, val owner: FirRegularClass)

abstract class GeneratedNestedClassIndex : FirSessionComponent {
    companion object {
        fun create(): GeneratedNestedClassIndex {
            return GeneratedNestedClassIndexImpl()
        }
    }

    abstract fun registerNestedClass(klass: FirRegularClass, owner: FirRegularClass)
    abstract operator fun get(key: FirPluginKey): List<GeneratedNestedClass>
}

val FirSession.generatedNestedClassIndex: GeneratedNestedClassIndex by FirSession.sessionComponentAccessor()

private class GeneratedNestedClassIndexImpl : GeneratedNestedClassIndex() {
    private val index: ArrayListMultimap<FirPluginKey, GeneratedNestedClass> = ArrayListMultimap.create()

    override fun registerNestedClass(klass: FirRegularClass, owner: FirRegularClass) {
        val key = (klass.origin as FirDeclarationOrigin.Plugin).key
        index.put(key, GeneratedNestedClass(klass, owner))
    }

    override fun get(key: FirPluginKey): List<GeneratedNestedClass> {
        return index.get(key)
    }
}