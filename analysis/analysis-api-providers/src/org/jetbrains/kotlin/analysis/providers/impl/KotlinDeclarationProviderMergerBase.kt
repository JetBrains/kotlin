/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderMerger
import org.jetbrains.kotlin.analysis.providers.impl.declarationProviders.CompositeKotlinDeclarationProvider

public abstract class KotlinDeclarationProviderMergerBase : KotlinDeclarationProviderMerger() {
    override fun mergeDeclarationProviders(declarationProviders: List<KotlinDeclarationProvider>): KotlinDeclarationProvider {
        // We should flatten declaration providers before merging so that the merger has access to all declaration providers, and also after
        // merging because composite declaration providers may be created by the merger.
        return CompositeKotlinDeclarationProvider.createFlattened(
            mergeToList(
                CompositeKotlinDeclarationProvider.flatten(declarationProviders)
            )
        )
    }

    protected abstract fun mergeToList(declarationProviders: List<KotlinDeclarationProvider>): List<KotlinDeclarationProvider>
}
