/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope

@DefaultImplementation(ImplicitsExtensionsResolutionFilter.Default::class)
interface ImplicitsExtensionsResolutionFilter {
    fun getScopesWithInfo(
        scopes: Sequence<HierarchicalScope>
    ): Sequence<ScopeWithImplicitsExtensionsResolutionInfo>

    object Default : ImplicitsExtensionsResolutionFilter {
        override fun getScopesWithInfo(
            scopes: Sequence<HierarchicalScope>
        ): Sequence<ScopeWithImplicitsExtensionsResolutionInfo> = scopes.map { scope ->
            ScopeWithImplicitsExtensionsResolutionInfo(scope, true)
        }
    }
}

class ScopeWithImplicitsExtensionsResolutionInfo(
    val scope: HierarchicalScope,
    val resolveExtensionsForImplicitReceiver: Boolean,
)
