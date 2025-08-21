/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

/**
 * A symbol provider function annotated with [LLModuleSpecificSymbolProviderAccess] is intended to be used in the context of the specific
 * [KaModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaModule] which owns the symbol provider. It requires all PSI arguments to
 * be contained in the scope of that module.
 *
 * When a PSI element is known, the responsible symbol provider can be queried directly. This is a common pattern in symbol provider code,
 * where we have narrowed down the search to a specific module. Module-specific access thus allows the symbol provider to avoid unnecessary
 * checks in a narrower, established context.
 *
 * If the symbol provider covers the whole module, the implementation may assume that the PSI element is in scope. However, if the symbol
 * provider has a narrower scope than its module, the implementation must take additional measures which ensure that the PSI element is
 * a valid input for the function.
 */
@RequiresOptIn("This function must be called in the context of the symbol provider's own `KaModule`. See the KDoc for details.")
internal annotation class LLModuleSpecificSymbolProviderAccess
