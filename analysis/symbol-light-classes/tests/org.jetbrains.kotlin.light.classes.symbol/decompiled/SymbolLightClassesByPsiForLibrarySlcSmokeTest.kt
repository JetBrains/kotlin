/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled

import org.junit.jupiter.api.Test

class SymbolLightClassesByPsiForLibrarySlcSmokeTest : AbstractSymbolLightClassesByPsiForLibrarySlcTest() {
    @Test
    fun typeAnnotations() {
        runTest("analysis/symbol-light-classes/testData/lightClassByPsi/typeAnnotations.kt")
    }
}
