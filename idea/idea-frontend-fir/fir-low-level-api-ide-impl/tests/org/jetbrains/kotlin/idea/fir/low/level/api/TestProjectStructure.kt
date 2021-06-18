/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.jsonUtils.getString
import java.nio.file.Path

internal data class TestProjectStructure(
    val modules: List<TestProjectModule>,
    val json: JsonObject,
) {
    companion object {
        fun parse(json: JsonElement): TestProjectStructure {
            require(json is JsonObject)

            return TestProjectStructure(
                json.getAsJsonArray("modules").map(TestProjectModule::parse),
                json,
            )
        }
    }
}


data class TestProjectModule(val name: String, val dependsOnModules: List<String>) {
    companion object {
        fun parse(json: JsonElement): TestProjectModule {
            require(json is JsonObject)
            val dependencies = if (json.has(DEPENDS_ON_FIELD)) {
                json.getAsJsonArray(DEPENDS_ON_FIELD).map { (it as JsonPrimitive).asString }
            } else emptyList()
            return TestProjectModule(
                json.getString("name"),
                dependencies
            )
        }

        private const val DEPENDS_ON_FIELD = "dependsOn"
    }
}

internal object TestProjectStructureReader {
    fun read(testDirectory: Path, jsonFileName: String = "structure.json"): TestProjectStructure {
        val jsonFile = testDirectory.resolve(jsonFileName)

        @Suppress("DEPRECATION") // AS 4.0
        val json = JsonParser().parse(FileUtil.loadFile(jsonFile.toFile(), /*convertLineSeparators=*/true))
        return TestProjectStructure.parse(json)
    }

    fun <T> readToTestStructure(
        testDirectory: Path,
        jsonFileName: String = "structure.json",
        toTestStructure: (TestProjectStructure) -> T,
    ): T = read(testDirectory, jsonFileName).let(toTestStructure)
}