/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaDeprecationLevel
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

@KaImplementationDetail
fun DeprecationLevelValue.toKaLevel(): KaDeprecationLevel {
    return when (this) {
        DeprecationLevelValue.WARNING -> KaDeprecationLevel.WARNING
        DeprecationLevelValue.ERROR -> KaDeprecationLevel.ERROR
        DeprecationLevelValue.HIDDEN -> KaDeprecationLevel.HIDDEN
    }
}
