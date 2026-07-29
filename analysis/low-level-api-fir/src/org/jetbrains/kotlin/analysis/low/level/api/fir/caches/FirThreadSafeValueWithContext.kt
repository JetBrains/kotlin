/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches

import org.jetbrains.kotlin.fir.caches.FirLazyValueWithContext
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * A thread-safe implementation of [FirLazyValueWithContext].
 *
 * We cannot use a standard lazy value here because we would have no way to thread the context into the initializer.
 */
internal class FirThreadSafeValueWithContext<V, CONTEXT>(
    createValue: (CONTEXT) -> V,
) : FirLazyValueWithContext<V, CONTEXT>() {
    private object NotComputed

    @Volatile
    private var createValue: ((CONTEXT) -> V)? = createValue

    @Volatile
    private var value: Any? = NotComputed

    // An artificial final field that ensures initialization safety (JLS 17.5): a `final` field forces a freeze at the end of the
    // constructor, which keeps the constructor's writes from being reordered past the publication of the instance. So a reader cannot
    // observe a partially initialized instance even when it is published unsafely, e.g. via a plain field. Without any final field, such
    // a reader might see the default values of `value` and `createValue`, and return `null` instead of the computed value.
    // `kotlin.SafePublicationLazyImpl` uses the same trick.
    @Suppress("unused")
    private val final: Any = NotComputed

    @Suppress("UNCHECKED_CAST")
    override fun getValue(context: CONTEXT): V {
        value.let { if (it !== NotComputed) return it as V }

        // A `null` `createValue` means another thread has already computed the value; we fall through to read it below.
        //
        // The context is only used for the first computation. Under a race, `createValue` may run more than once, but `compareAndSet`
        // ensures every thread observes the single winning value. `createValue` is released only by the thread that wins the CAS.
        val createValue = createValue
        if (createValue != null) {
            val newValue = createValue(context)
            val isSuccessful = valueUpdater.compareAndSet(this, NotComputed, newValue)
            if (isSuccessful) {
                this.createValue = null
                return newValue
            }
        }

        return value as V
    }

    companion object {
        private val valueUpdater = AtomicReferenceFieldUpdater.newUpdater(
            FirThreadSafeValueWithContext::class.java,
            Any::class.java,
            "value",
        )
    }
}
