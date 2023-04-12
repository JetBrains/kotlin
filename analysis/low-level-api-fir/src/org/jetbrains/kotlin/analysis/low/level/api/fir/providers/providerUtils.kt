/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import java.util.*

internal fun <T: Any> Optional<T>.getOrNull(): T? = orElse(null)

internal fun List<FirSymbolProvider>.createCombinedScope(): GlobalSearchScope {
    if (isEmpty()) return GlobalSearchScope.EMPTY_SCOPE
    return GlobalSearchScope.union(map { it.session.llFirModuleData.ktModule.contentScope })
}
