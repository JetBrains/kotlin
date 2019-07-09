package org.jetbrains.kotlin.idea.script

import java.io.File
import kotlin.script.dependencies.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.location.*
import kotlin.script.templates.ScriptTemplateDefinition

class FromTextDependenciesResolver : DependenciesResolver {
    @Suppress("UNCHECKED_CAST")
    override fun resolve(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult {
        return ScriptDependencies(
            javaHome = environment["javaHome"] as? File,
            classpath = (environment["classpath"] as? List<File>).orEmpty(),
            imports = (environment["imports"] as? List<String>).orEmpty(),
            sources = (environment["sources"] as? List<File>).orEmpty()
        ).asSuccess()
    }
}

@ScriptExpectedLocations([ScriptExpectedLocation.Everywhere])
@ScriptTemplateDefinition(FromTextDependenciesResolver::class, scriptFilePattern = "script.kts")
open class Template