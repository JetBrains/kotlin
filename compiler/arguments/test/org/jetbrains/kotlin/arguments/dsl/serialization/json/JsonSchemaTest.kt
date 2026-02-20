/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.serialization.json

import io.github.smiley4.schemakenerator.core.CoreSteps.initial
import io.github.smiley4.schemakenerator.jsonschema.JsonSchemaSteps.compileInlining
import io.github.smiley4.schemakenerator.jsonschema.JsonSchemaSteps.generateJsonSchema
import io.github.smiley4.schemakenerator.jsonschema.JsonSchemaSteps.withTitle
import io.github.smiley4.schemakenerator.jsonschema.data.TitleType
import io.github.smiley4.schemakenerator.serialization.SerializationSteps.analyzeTypeUsingKotlinxSerialization
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.compilerArgumentsLevel
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class JsonSchemaTest {

    private val currentJsonSchema
        get() = initial<KotlinCompilerArguments>()
            .analyzeTypeUsingKotlinxSerialization()
            .generateJsonSchema()
            .withTitle(TitleType.SIMPLE)
            .compileInlining()

    @Test
    fun jsonSchemaIsUpToDate() {
        assertEqualsToFile(
            Paths.get("testResources", "expectedData", "expected.schema.json").toFile(),
            currentJsonSchema.json.prettyPrint(),
            { it },
        ) {
            "Compiler JSON schema was changed, please also increment 'KotlinCompilerArguments.schemaVersion'"
        }
    }

    @Test
    fun jsonSchemaVersionIsUpdated() {
        val stubTopLevel by compilerArgumentsLevel("topLevel") {}
        val currentJsonSchemaVersion = KotlinCompilerArguments(topLevel = stubTopLevel).schemaVersion

        assertEquals(CURRENT_JSON_SCHEMA_VERSION, currentJsonSchemaVersion)
        assertEquals(CURRENT_JSON_SCHEMA_HASH, currentJsonSchema.json.prettyPrint().hashCode()) {
            "Current JSON schema version $currentJsonSchemaVersion was not updated. " +
                    "Please update it in 'KotlinCompilerArguments.schemaVersion' and in this test."
        }
    }

    companion object {
        private const val CURRENT_JSON_SCHEMA_VERSION = 6
        private const val CURRENT_JSON_SCHEMA_HASH = 1446859938
    }
}