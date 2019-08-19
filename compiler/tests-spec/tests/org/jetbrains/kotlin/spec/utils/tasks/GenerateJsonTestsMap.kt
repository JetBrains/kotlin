/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.tasks

import com.google.gson.JsonObject
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.MODULE_PATH
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.TESTDATA_PATH
import org.jetbrains.kotlin.spec.utils.TestsJsonMapBuilder
import org.jetbrains.kotlin.spec.utils.models.LinkedSpecTest
import org.jetbrains.kotlin.spec.utils.parsers.CommonParser
import java.io.File

private const val OUT_DIR = "out"
private const val OUT_FILENAME = "testsMap.json"

fun main() {
    val testsMap = JsonObject()

    File(TESTDATA_PATH).walkTopDown().forEach {
        val (specTest, _) = CommonParser.parseSpecTest(it.canonicalPath, mapOf("main.kt" to it.readText()))

        if (specTest is LinkedSpecTest)
            TestsJsonMapBuilder.buildJsonElement(specTest, testsMap)
    }

    val outDir = "$MODULE_PATH/$OUT_DIR"

    File(outDir).mkdir()
    File("$outDir/$OUT_FILENAME").writeText(testsMap.toString())
}