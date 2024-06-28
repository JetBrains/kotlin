/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.mock.MockProject
import com.intellij.openapi.util.Ref
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.cli.common.fir.SequentialPositionFinder
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment.Companion.createForTests
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.readSourceFileWithMapping
import org.jetbrains.kotlin.test.testFramework.KtPlatformLiteFixture
import org.junit.Assert
import java.io.ByteArrayInputStream

class LightTreeParsingTest : KtPlatformLiteFixture() {

    private var myEnvironment: KotlinCoreEnvironment? = null

    override fun setUp() {
        super.setUp()
        myEnvironment = createForTests(
            testRootDisposable, CompilerConfiguration.EMPTY,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        myProject = myEnvironment!!.project as MockProject
    }

    override fun tearDown() {
        super.tearDown()
        myProject = null
        myEnvironment = null
    }

    fun testLightTreeReadLineEndings() {

        data class LinePos(
            val mappingLine: Int,
            val line: Int,
            val col: Int,
            val content: String?,
        ) {
            override fun toString(): String = "$mappingLine: \"$content\" ($line:$col)"
        }

        fun String.makeCodeMappingAndPositions() = run {
            val (code, mapping) = ByteArrayInputStream(toByteArray()).reader().readSourceFileWithMapping()
            val positionFinder = SequentialPositionFinder(ByteArrayInputStream(toByteArray()).reader())
            val linePositions =
                LightTree2Fir.buildLightTree(code, null).getChildrenAsArray()
                    .mapNotNull { it?.startOffset }
                    .map {
                        val nextPos = positionFinder.findNextPosition(it)
                        LinePos( mapping.getLineByOffset(it), nextPos.line, nextPos.column, nextPos.lineContent)
                    }
            Triple(code.toString(), mapping, linePositions)
        }

        val (codeLf, mappingLf, positionsLf) = MULTILINE_SOURCE.makeCodeMappingAndPositions()

        val (codeCrLf, mappingCrLf, positionsCrLf) =
            MULTILINE_SOURCE.replace("\n", "\r\n").makeCodeMappingAndPositions()

        val (codeCrLfMixed, mappingCrLfMixed, positionsCrLfMixed) =
            MULTILINE_SOURCE.let {
                var toReplace = false
                buildString {
                    it.forEach { c ->
                        if (c == '\n') {
                            if (toReplace) append("\r\n") else append(c)
                            toReplace = !toReplace
                        } else append(c)
                    }
                }
            }.also { s ->
                Assert.assertEquals(s.count { it == '\r' }, s.count { it == '\n' } / 2)
            }.makeCodeMappingAndPositions()

        // classic MacOS line endings are probably not to be found in the wild, but checking the support nevertheless
        val (codeCr, mappingCr, positionsCr) =
            MULTILINE_SOURCE.replace("\n", "\r").makeCodeMappingAndPositions()

        Assert.assertEquals(codeLf, codeCrLf)
        Assert.assertEquals(codeLf, codeCrLfMixed)
        Assert.assertEquals(codeLf, codeCr)

        Assert.assertEquals(mappingLf.linesCount, mappingCrLf.linesCount)
        Assert.assertEquals(mappingLf.linesCount, mappingCrLfMixed.linesCount)
        Assert.assertEquals(mappingLf.linesCount, mappingCr.linesCount)

        Assert.assertEquals(positionsLf, positionsCrLf)
        Assert.assertEquals(positionsLf, positionsCrLfMixed)
        Assert.assertEquals(positionsLf, positionsCr)

        Assert.assertEquals(mappingLf.lastOffset, mappingCrLf.lastOffset)
        Assert.assertEquals(mappingLf.lastOffset, mappingCrLfMixed.lastOffset)
        Assert.assertEquals(mappingLf.lastOffset, mappingCr.lastOffset)
    }
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.getChildrenAsArray(): Array<out LighterASTNode?> {
    val kidsRef = Ref<Array<LighterASTNode?>>()
    getChildren(root, kidsRef)
    return kidsRef.get()
}

private const val MULTILINE_SOURCE = """
val a = 1
 val b = 2 
  val c = 3
   val d = 4
    val e = 5
"""