/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

/**
 * A marker interface for a provider that can be composed, i.e. multiple instances of the same provider can be composed into a single
 * provider.
 *
 * Composable providers share certain traits: There is a notion of a sequentially composed [KotlinCompositeProvider] of that kind, and there
 * is usually a merge function which allows to create a single provider from a list of providers. Mergers of composable providers may
 * produce a merged provider which is more efficient than the naive sequential composite provider.
 *
 * @see KotlinDeclarationProvider
 * @see KotlinPackageProvider
 */
public interface KotlinComposableProvider

/**
 * A [KotlinCompositeProvider] is the sequential composition of a specific kind of composable provider [P].
 *
 * A composite provider should only contain providers of the same base type as the composite provider itself, so implementations of
 * [KotlinCompositeProvider] should always be a subtype of their type argument [P]. (This is not enforceable in the Kotlin type system.)
 */
public interface KotlinCompositeProvider<P : KotlinComposableProvider> : KotlinComposableProvider {
    public val providers: List<P>
}

public interface KotlinComposableProviderMerger<P : KotlinComposableProvider> {
    /**
     * Merges the given [providers] into a single provider. When possible, mergers will try to create a provider that is more efficient
     * compared to the naive sequential composite provider. Not all providers might be mergeable, or there might be multiple separate sets
     * of providers that can be merged individually, so the resulting provider may be a composite provider.
     */
    public fun merge(providers: List<P>): P
}
