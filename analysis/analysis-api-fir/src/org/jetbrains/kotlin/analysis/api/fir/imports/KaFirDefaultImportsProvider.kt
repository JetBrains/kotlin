/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.imports

import org.jetbrains.kotlin.analysis.api.impl.base.imports.KaBaseDefaultImportsProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.imports.defaultImportsProvider
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.DefaultImportsProvider

// K1 implementation is in IDE: `org.jetbrains.kotlin.base.fe10.analysis.KaFe10DefaultImportsProvider`
internal class KaFirDefaultImportsProvider : KaBaseDefaultImportsProvider() {
    override fun getCompilerDefaultImportsProvider(targetPlatform: TargetPlatform): DefaultImportsProvider =
        targetPlatform.defaultImportsProvider
}
