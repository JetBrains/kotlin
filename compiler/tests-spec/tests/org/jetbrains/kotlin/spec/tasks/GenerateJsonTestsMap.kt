/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.tasks

import com.google.gson.JsonObject
import org.jetbrains.kotlin.spec.models.LinkedSpecTest
import org.jetbrains.kotlin.spec.parsers.CommonParser
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.MODULE_PATH
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.TESTDATA_PATH
import org.jetbrains.kotlin.spec.utils.TestsJsonMapBuilder
import java.io.File

private const val OUT_DIR = "out"
private const val OUT_FILENAME = "testsMap.json"

fun main(args: Array<String>) {
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