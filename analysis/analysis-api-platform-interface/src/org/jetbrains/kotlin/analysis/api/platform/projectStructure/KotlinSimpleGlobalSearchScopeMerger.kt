/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.psi.search.GlobalSearchScope

/**
 * A non-optimizing [KotlinGlobalSearchScopeMerger] which simply creates naive union scopes. It may be registered if the platform cannot
 * provide any scope merging strategies.
 */
public class KotlinSimpleGlobalSearchScopeMerger : KotlinGlobalSearchScopeMerger {
    override fun union(scopes: Collection<GlobalSearchScope>): GlobalSearchScope {
        if (scopes.isEmpty()) {
            return GlobalSearchScope.EMPTY_SCOPE
        }
        return GlobalSearchScope.union(scopes.toList())
    }
}
