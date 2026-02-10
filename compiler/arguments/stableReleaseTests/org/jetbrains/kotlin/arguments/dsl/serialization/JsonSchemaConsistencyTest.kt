/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.serialization

import io.github.smiley4.schemakenerator.core.CoreSteps.initial
import io.github.smiley4.schemakenerator.jsonschema.JsonSchemaSteps.compileInlining
import io.github.smiley4.schemakenerator.jsonschema.JsonSchemaSteps.generateJsonSchema
import io.github.smiley4.schemakenerator.jsonschema.JsonSchemaSteps.withTitle
import io.github.smiley4.schemakenerator.jsonschema.data.TitleType
import io.github.smiley4.schemakenerator.serialization.SerializationSteps.analyzeTypeUsingKotlinxSerialization
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArguments
import org.jetbrains.kotlin.arguments.stable.description.kotlinCompilerArguments as stableKotlinCompilerArguments
import org.jetbrains.kotlin.arguments.stable.dsl.base.KotlinCompilerArguments as StableKotlinCompilerArguments
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class JsonSchemaConsistencyTest {
    private val currentJsonSchema
        get() = initial<KotlinCompilerArguments>()
            .analyzeTypeUsingKotlinxSerialization()
            .generateJsonSchema()
            .withTitle(TitleType.SIMPLE)
            .compileInlining()

    private val previousJsonSchema
        get() = initial<StableKotlinCompilerArguments>()
            .analyzeTypeUsingKotlinxSerialization()
            .generateJsonSchema()
            .withTitle(TitleType.SIMPLE)
            .compileInlining()

    @Test
    fun jsonSchemaVersionIsUpdated() {
        if (currentJsonSchema != previousJsonSchema) {
            assertTrue(
                actual = kotlinCompilerArguments.schemaVersion > stableKotlinCompilerArguments.schemaVersion,
                message = "Current JSON schema version ${kotlinCompilerArguments.schemaVersion} was not updated. " +
                        "Please update it to be higher than ${stableKotlinCompilerArguments.schemaVersion}."
            )
        }
    }

}