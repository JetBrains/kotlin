/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.benchmark.generators

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.impl.DebugUtil
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.builder.RawFirBuilderMode
import org.jetbrains.kotlin.psi.KtFile
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import java.io.File

@State(Scope.Benchmark)
open class Psi2FirGenerator : TreeGenerator, AbstractRawFirBuilderTestCase() {
    override fun generateBaseTree(text: String, file: File) {
        val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.path)), text) as KtFile
        DebugUtil.psiTreeToString(ktFile, false)
    }

    override fun generateFir(text: String, file: File, stubMode: Boolean) {
        val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.path)), text) as KtFile
        val firFile = ktFile.toFirFile(RawFirBuilderMode.stubs(stubMode))
        StringBuilder().also { FirRenderer(it).visitFile(firFile) }.toString()
    }

    override fun setUp() {
        super.setUp()
    }

    override fun tearDown() {
        super.tearDown()
    }
}