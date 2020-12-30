/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.google.common.collect.ImmutableMap
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

interface FirBindingContextValueProviders {
    fun <K : Any?, V : Any?> getIfPossible(slice: ReadOnlySlice<K, V>?, key: K): V?
}

class FirBasedBindingContext : BindingContext {
    private val callAndResolverCallWrappers = CallAndResolverCallWrappers()

    private val valueProviders = listOf<FirBindingContextValueProviders>(callAndResolverCallWrappers)
    override fun getDiagnostics(): Diagnostics {
        TODO("Not yet implemented")
    }

    override fun <K : Any?, V : Any?> get(slice: ReadOnlySlice<K, V>?, key: K): V? =
        valueProviders.firstNotNullResult { it.getIfPossible(slice, key) }

    override fun <K : Any?, V : Any?> getKeys(slice: WritableSlice<K, V>?): Collection<K> {
        TODO("Not yet implemented")
    }

    override fun <K : Any?, V : Any?> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> {
        TODO("Not yet implemented")
    }

    override fun getType(expression: KtExpression): KotlinType? {
        TODO("Not yet implemented")
    }

    override fun addOwnDataTo(trace: BindingTrace, commitDiagnostics: Boolean) {
        TODO("Not yet implemented")
    }
}