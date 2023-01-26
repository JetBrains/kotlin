/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.readSourceFileWithMapping
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream

class LightTreeParsingTest {

    @Test
    fun testLightTreeReadLineEndings() {
        fun String.makeCodeMappingAndLines() = run {
            val (code, mapping) = ByteArrayInputStream(toByteArray()).reader().readSourceFileWithMapping()
            val lines =
                LightTree2Fir.buildLightTree(code).getChildrenAsArray().mapNotNull { it?.startOffset }.map { mapping.getLineByOffset(it) }
            Triple(code, mapping, lines)
        }

        val (codeFromTextWithLf, mappingFromTextWithLf, linesFromTextWithLf) = MULTILINE_SOURCE.makeCodeMappingAndLines()

        val (codeFromTextWithCrLf, mappingFromTextWithCrLf, linesFromTextWithCrLf) =
            MULTILINE_SOURCE.replace("\n", "\r\n").makeCodeMappingAndLines()

        // classic Mac OS line endings are probably not to be found in the wild, but checking the support nevertheless
        val (codeFromTextWithCr, mappingFromTextWithCr, linesFromTextWithCr) =
            MULTILINE_SOURCE.replace("\n", "\r").makeCodeMappingAndLines()

        Assert.assertEquals(codeFromTextWithLf.toString(), codeFromTextWithCrLf.toString())
        Assert.assertEquals(codeFromTextWithLf.toString(), codeFromTextWithCr.toString())

        Assert.assertEquals(mappingFromTextWithLf.linesCount, mappingFromTextWithCrLf.linesCount)
        Assert.assertEquals(mappingFromTextWithLf.linesCount, mappingFromTextWithCr.linesCount)

        Assert.assertEquals(linesFromTextWithLf, linesFromTextWithCrLf)
        Assert.assertEquals(linesFromTextWithLf, linesFromTextWithCr)

        Assert.assertEquals(mappingFromTextWithLf.lastOffset, mappingFromTextWithCrLf.lastOffset)
        Assert.assertEquals(mappingFromTextWithLf.lastOffset, mappingFromTextWithCr.lastOffset)
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
"""