/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.old.binding

import com.google.common.collect.ImmutableMap
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.idea.frontend.old.KtSymbolBasedContext
import org.jetbrains.kotlin.idea.frontend.old.toKotlinType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

class KtSymbolBasedBindingContext(val context: KtSymbolBasedContext) : BindingContext {
    private val LOG = Logger.getInstance(KtSymbolBasedBindingContext::class.java)

    private val getterBySlice: MutableMap<ReadOnlySlice<*, *>, (Nothing) -> Any?> = hashMapOf()

    init {
        CallAndResolverCallWrappers(this)
        ToDescriptorBindingContextValueProviders(this)
    }

    fun <K, V> registerGetterByKey(slice: ReadOnlySlice<K, V>, getter: (K) -> V?) {
        check(!getterBySlice.containsKey(slice)) {
            "Key $slice already registered: ${getterBySlice[slice]}"
        }
        getterBySlice[slice] = getter
    }

    override fun getDiagnostics(): Diagnostics = context.incorrectImplementation { Diagnostics.EMPTY }

    override fun <K : Any?, V : Any?> get(slice: ReadOnlySlice<K, V>, key: K): V? {
        val getter = getterBySlice[slice]
        if (getter == null) {
            LOG.info("Key not registered: $slice")
            return null
        }

        @Suppress("UNCHECKED_CAST")
        return (getter as (K) -> V?)(key)
    }

    override fun getType(expression: KtExpression): KotlinType? =
        with(context.ktAnalysisSession) {
            expression.getKtType().toKotlinType(context)
        }

    override fun <K : Any?, V : Any?> getKeys(slice: WritableSlice<K, V>?): Collection<K> =
        context.noImplementation()

    override fun <K : Any?, V : Any?> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> =
        context.noImplementation()

    override fun addOwnDataTo(trace: BindingTrace, commitDiagnostics: Boolean) =
        context.noImplementation()
}