/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.tasks

import com.google.gson.JsonObject
import org.jetbrains.kotlin.spec.TestsJsonMapBuilder
import org.jetbrains.kotlin.spec.validators.LinkedSpecTestValidator
import org.jetbrains.kotlin.spec.validators.SpecTestValidationException
import java.io.File

private const val TEST_DATA_DIR = "./testData"
private const val OUT_DIR = "./out"
private const val OUT_FILENAME = "testsMap.json"

fun main(args: Array<String>) {
    val testsMap = JsonObject()

    File(TEST_DATA_DIR).walkTopDown().forEach {
        val specTestValidator = LinkedSpecTestValidator(it)

        try {
            specTestValidator.parseTestInfo()
        } catch (e: SpecTestValidationException) {
            return@forEach
        }

        TestsJsonMapBuilder.buildJsonElement(specTestValidator.testInfo, testsMap)
    }

    File(OUT_DIR).mkdir()
    File("$OUT_DIR/$OUT_FILENAME").writeText(testsMap.toString())
}