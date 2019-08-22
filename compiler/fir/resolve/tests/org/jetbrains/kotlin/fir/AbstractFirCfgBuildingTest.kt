/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.renderToStringBuilder
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractFirCfgBuildingTest : AbstractFirResolveTestCase() {
    override val configurationKind: ConfigurationKind
        get() = ConfigurationKind.ALL

    override fun doTest(path: String) {
        val firFiles = processInputFile(path)
        checkCfg(path, firFiles)
        checkFir(path, firFiles)
    }

    fun checkCfg(path: String, firFiles: List<FirFile>) {
        val firFileDump = StringBuilder().also { firFiles.first().accept(FirControlFlowGraphRenderVisitor(it), null) }.toString()
        val expectedPath = path.replace(".kt", ".cfg.txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), firFileDump)
    }

    private class FirControlFlowGraphRenderVisitor(private val builder: StringBuilder) : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {
            (controlFlowGraphReference as? FirControlFlowGraphReferenceImpl)?.controlFlowGraph?.renderToStringBuilder(builder)
        }
    }
}