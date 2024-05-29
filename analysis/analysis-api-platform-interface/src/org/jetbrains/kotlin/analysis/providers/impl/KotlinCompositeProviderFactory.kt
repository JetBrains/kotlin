/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import org.jetbrains.kotlin.analysis.providers.KotlinComposableProvider
import org.jetbrains.kotlin.analysis.providers.KotlinCompositeProvider
import org.jetbrains.kotlin.analysis.providers.impl.util.mergeOnly

/**
 * [KotlinCompositeProviderFactory] is used by various [KotlinCompositeProvider]s to share code related to provider creation and flattening.
 */
public class KotlinCompositeProviderFactory<P : KotlinComposableProvider>(
    private val emptyProvider: P,
    private val composeProviders: (List<P>) -> P,
) {
    public fun create(providers: List<P>): P = when (providers.size) {
        0 -> emptyProvider
        1 -> providers.single()
        else -> composeProviders(providers)
    }

    public fun createFlattened(providers: List<P>): P =
        create(if (providers.size > 1) flatten(providers) else providers)

    public fun flatten(providers: List<P>): List<P> =
        providers.flatMap { provider ->
            // `KotlinCompositeProvider<P>` should always be a provider of type `P` itself, so the cast is legal. Still, suppressing the
            // error is unfortunate, but to properly type this, we'd need to be able to restrict `KotlinCompositeProvider<P>` to be a
            // subtype of `P`, which is not possible in Kotlin.
            @Suppress("UNCHECKED_CAST")
            when (provider) {
                is KotlinCompositeProvider<*> -> (provider as KotlinCompositeProvider<P>).providers
                else -> listOf(provider)
            }
        }
}

/**
 * Uses the given [factory] to merge all providers of type [T] with the given [mergeTargets] strategy. Other providers (not of type [T]) are
 * added to the resulting composite provider unmerged.
 */
public inline fun <P : KotlinComposableProvider, reified T : P> List<P>.mergeSpecificProviders(
    factory: KotlinCompositeProviderFactory<P>,
    crossinline mergeTargets: (List<T>) -> P,
): P {
    // We should flatten providers before merging so that the merger has access to all individual providers, and also after merging because
    // composite providers may be created by the merger.
    return factory.createFlattened(factory.flatten(this).mergeOnly<_, T> { mergeTargets(it) })
}
