package org.jetbrains.kotlin.idea.script

import java.io.File
import kotlin.script.dependencies.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.location.*
import kotlin.script.templates.ScriptTemplateDefinition

class FromTextDependenciesResolver : AsyncDependenciesResolver {
    @Suppress("UNCHECKED_CAST")
    suspend override fun resolveAsync(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult {
        return ScriptDependencies(
            classpath = (environment["classpath"] as? List<File>).orEmpty(),
            imports = (environment["imports"] as? List<String>).orEmpty(),
            sources = (environment["sources"] as? List<File>).orEmpty()
        ).asSuccess()
    }
}

@ScriptExpectedLocations([ScriptExpectedLocation.Everywhere])
@ScriptTemplateDefinition(FromTextDependenciesResolver::class, scriptFilePattern = "script.kts")
open class Template