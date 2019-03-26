/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.lang.impl.PsiBuilderFactoryImpl
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.parsing.KotlinParser
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.testFramework.KtParsingTestCase
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureNanoTime

/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class BaseTestCase: KtParsingTestCase(
    "",
    "kt",
    KotlinParserDefinition()
) {
    fun test() {
        val file = KtFile((PsiManager.getInstance(myProject) as PsiManagerEx).fileManager.createFileViewProvider(
            LightVirtualFile(
                "foo",
                KotlinFileType.INSTANCE,
                ""
            ), true
        ), false)
        val parserDefinition = KotlinParserDefinition()
        val lexer = parserDefinition.createLexer(myProject)
        val builder = PsiBuilderFactoryImpl().createBuilder(parserDefinition, lexer, "class Base { val x: Int = 1 }")

        val ktParsing = parserDefinition.createParser(myProject) as KotlinParser
        ktParsing.parse(null, builder, file)

        println(DebugUtil.lightTreeToString(builder.lightTree, false))
        println(DebugUtil.nodeTreeToString(builder.treeBuilt, false))
    }

    fun testTotalKotlinWithGivenMode() {
        val path = System.getProperty("user.dir")
        val root = File(path)
        var counter = 0
        var time = 0L

        val ktFile = KtFile((PsiManager.getInstance(myProject) as PsiManagerEx).fileManager.createFileViewProvider(
            LightVirtualFile(
                "foo",
                KotlinFileType.INSTANCE,
                ""
            ), true
        ), false)
        val parserDefinition = KotlinParserDefinition()
        val lexer = parserDefinition.createLexer(myProject)
        val parser = parserDefinition.createParser(myProject) as KotlinParser

        println("BASE PATH: $path")
        for (file in root.walkTopDown()) {
            if (file.isDirectory) continue
            if (file.path.contains("testData") || file.path.contains("resources")) continue
            if (file.extension != "kt") continue

            time += measureNanoTime {
                val builder = PsiBuilderFactoryImpl().createBuilder(parserDefinition, lexer, file.readText())
                parser.parse(null, builder, ktFile)

                DebugUtil.lightTreeToString(builder.lightTree, false)
            }

            counter++
        }
        println("SUCCESS!")
        println("TIME PER FILE: ${(time / counter) * 1e-6} ms, COUNTER: $counter")

    }
}

