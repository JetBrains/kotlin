package org.jetbrains.kotlin.idea.script

import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.ScriptingHostConfigurationKeys
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.util.PropertiesCollection

@KotlinScript(
    displayName = "Definition for tests",
    fileExtension = "kts",
    compilationConfiguration = TemplateDefinition::class
)
open class Template(val args: Array<String>)

val ScriptingHostConfigurationKeys.getEnvironment by PropertiesCollection.key<() -> Map<String, Any?>?>()

@Suppress("UNCHECKED_CAST")
object TemplateDefinition : ScriptCompilationConfiguration(
    {
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
        refineConfiguration {
            beforeCompiling { context ->
                val environment = context.compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration]?.let {
                    it[ScriptingHostConfiguration.getEnvironment]?.invoke()
                }.orEmpty()

                context.compilationConfiguration.with {
                    (environment["javaHome"] as? File)?.let {
                        jvm.jdkHome(it)
                    }
                    (environment["imports"] as? List<String>)?.let {
                        defaultImports(it)
                    }
                    (environment["classpath"] as? List<File>)?.let {
                        dependencies(JvmDependency(it))
                    }
                    (environment["sources"] as? List<File>)?.let {
                        ide.dependenciesSources(JvmDependency(it))
                    }
                }.asSuccess()
            }
        }
    }
)