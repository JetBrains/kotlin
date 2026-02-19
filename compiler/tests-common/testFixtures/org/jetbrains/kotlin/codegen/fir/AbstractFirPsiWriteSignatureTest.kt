/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.fir

import org.jetbrains.kotlin.codegen.ir.AbstractIrWriteSignatureTest
import org.jetbrains.kotlin.test.FirParser

abstract class AbstractFirPsiWriteSignatureTest : AbstractIrWriteSignatureTest() {
    override val useFir: Boolean
        get() = true

    override val firParser: FirParser
        get() = FirParser.Psi
}
