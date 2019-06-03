/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.benchmark.generators

import org.jetbrains.kotlin.fir.antlr2fir.Antlr2Fir
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import java.io.File

open class Antlr2FirGenerator : TreeGenerator, AbstractRawFirBuilderTestCase() {
    override fun generateBaseTree(text: String, file: File) {
        Antlr2Fir(stubMode = true).buildAntlrOnly(text)
    }

    override fun generateFir(text: String, file: File) {
        Antlr2Fir(stubMode = true).buildFirFile(text)
    }

    override fun setUp() {
        super.setUp()
    }

    override fun tearDown() {
        super.tearDown()
    }
}