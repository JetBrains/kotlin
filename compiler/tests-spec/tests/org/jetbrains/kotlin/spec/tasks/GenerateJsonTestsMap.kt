/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.tasks

import com.google.gson.JsonObject
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.MODULE_PATH
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.TESTDATA_PATH
import org.jetbrains.kotlin.spec.utils.TestsJsonMapBuilder
import org.jetbrains.kotlin.spec.validators.LinkedSpecTestValidator
import org.jetbrains.kotlin.spec.validators.SpecTestValidationException
import java.io.File

private const val OUT_DIR = "out"
private const val OUT_FILENAME = "testsMap.json"

fun main(args: Array<String>) {
    val testsMap = JsonObject()

    File(TESTDATA_PATH).walkTopDown().forEach {
        val specTestValidator = LinkedSpecTestValidator(it)

        try {
            specTestValidator.parseTestInfo()
        } catch (e: SpecTestValidationException) {
            return@forEach
        }

        TestsJsonMapBuilder.buildJsonElement(specTestValidator.testInfo, testsMap)
    }

    val outDir = "$MODULE_PATH/$OUT_DIR"

    File(outDir).mkdir()
    File("$outDir/$OUT_FILENAME").writeText(testsMap.toString())
}