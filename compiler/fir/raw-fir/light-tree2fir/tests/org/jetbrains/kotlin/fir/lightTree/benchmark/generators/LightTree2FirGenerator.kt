/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.benchmark.generators

import com.intellij.psi.impl.DebugUtil
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.builder.StubFirScopeProvider
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import java.io.File

@State(Scope.Benchmark)
open class LightTree2FirGenerator : TreeGenerator, AbstractRawFirBuilderTestCase() {
    override fun generateBaseTree(text: String, file: File) {
        val lightTreeConverter = LightTree2Fir(
            session = FirSessionFactory.createEmptySession(),
            scopeProvider = StubFirScopeProvider,
            stubMode = false
        )
        val lightTree = lightTreeConverter.buildLightTree(text)
        DebugUtil.lightTreeToString(lightTree, false)
    }

    override fun generateFir(text: String, file: File, stubMode: Boolean) {
        val lightTreeConverter = LightTree2Fir(
            session = FirSessionFactory.createEmptySession(),
            scopeProvider = StubFirScopeProvider,
            stubMode = stubMode
        )
        val firFile = lightTreeConverter.buildFirFile(text, file.name)
        StringBuilder().also { FirRenderer(it).visitFile(firFile) }.toString()
    }

    override fun setUp() {
        super.setUp()
    }

    override fun tearDown() {
        super.tearDown()
    }
}
