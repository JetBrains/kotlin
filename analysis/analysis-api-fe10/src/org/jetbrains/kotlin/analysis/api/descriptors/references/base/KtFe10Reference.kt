/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.references.base

import com.intellij.psi.impl.source.resolve.ResolveCache
import org.jetbrains.kotlin.analysis.api.KtSymbolBasedReference
import org.jetbrains.kotlin.idea.references.KtReference

interface KtFe10Reference : KtReference, KtSymbolBasedReference {
    override val resolver: ResolveCache.PolyVariantResolver<KtReference>
        get() = KtFe10PolyVariantResolver
}