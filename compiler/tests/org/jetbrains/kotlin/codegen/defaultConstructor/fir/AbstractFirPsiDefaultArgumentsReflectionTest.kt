/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.defaultConstructor.fir

import org.jetbrains.kotlin.codegen.defaultConstructor.ir.AbstractIrDefaultArgumentsReflectionTest
import org.jetbrains.kotlin.test.FirParser

abstract class AbstractFirPsiDefaultArgumentsReflectionTest : AbstractIrDefaultArgumentsReflectionTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = FirParser.Psi
}
