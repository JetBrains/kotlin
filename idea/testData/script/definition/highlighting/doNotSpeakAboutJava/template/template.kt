package custom.scriptDefinition

import kotlin.script.dependencies.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.templates.*
import java.io.File

class TestDependenciesResolver : DependenciesResolver {
    override fun resolve(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult {
        val reports = ArrayList<ScriptReport>()
        scriptContents.text?.let { text ->
            text.lines().forEachIndexed { lineIndex, line ->
                val adjustedLine = line.replace(Regex("(<error descr=\"Can't use\">)|(</error>)|(<warning descr=\"Shouldn't use\">)|(</warning>)"), "")
                Regex("java").findAll(adjustedLine).forEach {
                    reports.add(
                            ScriptReport(
                                    "Can't use",
                                    ScriptReport.Severity.ERROR,
                                    ScriptReport.Position(lineIndex, it.range.first, lineIndex, it.range.last + 1)
                            )
                    )
                }
                Regex("scala").findAll(adjustedLine).forEach {
                    reports.add(
                            ScriptReport(
                                    "Shouldn't use",
                                    ScriptReport.Severity.WARNING,
                                    ScriptReport.Position(lineIndex, it.range.first, lineIndex, it.range.last + 1)
                            )
                    )
                }
            }
        }

        return DependenciesResolver.ResolveResult.Success(
                ScriptDependencies(
                        classpath = listOf(environment["template-classes"] as File)
                ),
                reports
        )
    }
}

@ScriptTemplateDefinition(TestDependenciesResolver::class, scriptFilePattern = "script.kts")
class Template : Base()

open class Base {
    val i = 3
    val str = ""
}