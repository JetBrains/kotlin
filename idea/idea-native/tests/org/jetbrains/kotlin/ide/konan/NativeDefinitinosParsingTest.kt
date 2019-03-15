/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.testFramework.ParsingTestCase


class NativeDefinitionsParsingTest : ParsingTestCase("", "def", NativeDefinitionsParserDefinition()) {

    fun testAllProperties() = doTest(true)

    fun testBadDefinitions() = doTest(true)

    override fun getTestDataPath(): String = "testData/colorHighlighting"

    override fun skipSpaces(): Boolean = false

    override fun includeRanges(): Boolean = true
}