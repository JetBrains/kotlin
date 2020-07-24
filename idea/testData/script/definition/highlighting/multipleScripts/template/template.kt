package custom.scriptDefinition

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

val ScriptingHostConfigurationKeys.getEnvironment by PropertiesCollection.key<() -> Map<String, Any?>?>()

@KotlinScript(
    displayName = "Definition for scriptFirstLoaded.mykts",
    fileExtension = "mykts",
    compilationConfiguration = DefinitionForFirstLoadedScript::class
)
open class TemplateForFirstLoadedScript(val args: Array<String>)

@Suppress("UNCHECKED_CAST")
object DefinitionForFirstLoadedScript : ScriptCompilationConfiguration(
    {
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
        refineConfiguration {
            beforeCompiling { context ->
                val environment = context.compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration]?.let {
                    it[ScriptingHostConfiguration.getEnvironment]?.invoke()
                }.orEmpty()

                // lib-classes as script dependencies: they will be the first in allScriptDependenciesClassFiles
                context.compilationConfiguration.with {
                    dependencies(JvmDependency(environment["lib-classes"] as File))
                }.asSuccess()
            }
        }
    }
)

@KotlinScript(
    displayName = "Definition for main script",
    fileExtension = "kts",
    compilationConfiguration = DefinitionForMainScript::class
)
open class TemplateForMainScript(val args: Array<String>)

@Suppress("UNCHECKED_CAST")
object DefinitionForMainScript : ScriptCompilationConfiguration(
    {
        baseClass(custom.project.Project::class)
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
        refineConfiguration {
            beforeCompiling { context ->
                val environment = context.compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration]?.let {
                    it[ScriptingHostConfiguration.getEnvironment]?.invoke()
                }.orEmpty()

                // template-classes as script dependencies: they will be the second in allScriptDependenciesClassFiles
                context.compilationConfiguration.with {
                    dependencies(JvmDependency(environment["template-classes"] as File))

                    implicitReceivers(custom.project.Project::class)
                }.asSuccess()
            }
        }
    }
)

