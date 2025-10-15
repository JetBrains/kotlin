/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.kmp.infra.LightTreeTestParser
import org.jetbrains.kotlin.kmp.infra.ParseMode
import org.jetbrains.kotlin.kmp.infra.TestParseNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullParserTestsWithLightTree : AbstractParserTests<LighterASTNode>() {
    private val lightTreeTestParser = LightTreeTestParser()

    override val parseMode: ParseMode = ParseMode.NoKDoc

    override fun recognizeOldSyntaxElement(fileName: String, text: String): TestParseNode<LighterASTNode> =
        lightTreeTestParser.parse(fileName, text)

    override val oldRecognizerSuffix: String = " (LightTree)"

    @AfterAll
    fun cleanup() {
        lightTreeTestParser.dispose()
    }
}