package custom.scriptDefinition

import kotlin.script.dependencies.*
import kotlin.script.templates.*
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class TestDependenciesResolver : ScriptDependenciesResolver {
    override fun resolve(
            script: ScriptContents,
            environment: Map<String, Any?>?,
            report: (ScriptDependenciesResolver.ReportSeverity, String, ScriptContents.Position?) -> Unit, previousDependencies: KotlinScriptExternalDependencies?
    ): Future<KotlinScriptExternalDependencies?> {
        script.text?.let { text ->
            text.lines().forEachIndexed { lineIndex, line ->
                val adjustedLine = line.replace(Regex("(<error descr=\"Can't use\">)|(</error>)|(<warning descr=\"Shouldn't use\">)|(</warning>)"), "")
                Regex("java").findAll(adjustedLine).forEach {
                    val columnIndex = it.range.first
                    report(ScriptDependenciesResolver.ReportSeverity.ERROR, "Can't use", ScriptContents.Position(lineIndex, columnIndex))
                }
                Regex("scala").findAll(adjustedLine).forEach {
                    val columnIndex = it.range.first
                    report(ScriptDependenciesResolver.ReportSeverity.WARNING, "Shouldn't use", ScriptContents.Position(lineIndex, columnIndex))
                }
            }
        }


        return CompletableFuture.completedFuture(
                object : KotlinScriptExternalDependencies {
                    override val classpath: Iterable<File> = listOf(environment?.get("template-classes") as File)
                })

    }
}

@ScriptTemplateDefinition(TestDependenciesResolver::class, scriptFilePattern = "script.kts")
class Template : Base()

open class Base {
    val i = 3
    val str = ""
}