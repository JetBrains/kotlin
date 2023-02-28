/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.scripting

import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm

private const val SCRIPT_FILE_LOCATION_DEFAULT_VARIABLE_NAME = "__FILE__"

@Suppress("unused")
@KotlinScript(
    fileExtension = "irTest.kts",
    compilationConfiguration = KotlinIrTestScriptCompilationConfiguration::class,
)
abstract class KotlinIrTestScript {
    fun foo() {}
}

object KotlinIrTestScriptCompilationConfiguration : ScriptCompilationConfiguration(
    {
        defaultImports(Requires::class)
        jvm {
            dependenciesFromClassContext(KotlinIrTestScript::class, "kotlin-stdlib", "kotlin-reflect")
        }
        refineConfiguration {
            beforeCompiling(::configureScriptFileLocationPathVariablesForCompilation)
        }
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
    }
)

fun configureScriptFileLocationPathVariablesForCompilation(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val scriptFile = (context.script as? FileBasedScriptSource)?.file ?: return context.compilationConfiguration.asSuccess()
    val scriptFileLocationVariableName = context.compilationConfiguration[ScriptCompilationConfiguration.scriptFileLocationVariable]
        ?: SCRIPT_FILE_LOCATION_DEFAULT_VARIABLE_NAME

    return ScriptCompilationConfiguration(context.compilationConfiguration) {
        providedProperties.put(mapOf(scriptFileLocationVariableName to KotlinType(File::class)))
        scriptFileLocation.put(scriptFile)
        scriptFileLocationVariable.put(scriptFileLocationVariableName)
    }.asSuccess()
}
