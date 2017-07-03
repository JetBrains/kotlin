/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")

package kotlin.script.dependencies

import java.io.File
import kotlin.script.dependencies.DependenciesResolver.ResolveResult

typealias Environment = Map<String, Any?>

interface DependenciesResolver : @Suppress("DEPRECATION") ScriptDependenciesResolver {
    fun resolve(scriptContents: ScriptContents, environment: Environment): ResolveResult

    object NoDependencies : DependenciesResolver {
        override fun resolve(scriptContents: ScriptContents, environment: Environment) = ScriptDependencies.Empty.asSuccess()
    }

    sealed class ResolveResult {
        abstract val dependencies: ScriptDependencies?
        abstract val reports: List<ScriptReport>

        data class Success(
                override val dependencies: ScriptDependencies,
                override val reports: List<ScriptReport> = listOf()
        ) : ResolveResult()

        data class Failure(override val reports: List<ScriptReport>) : ResolveResult() {
            constructor(vararg reports: ScriptReport) : this(reports.asList())

            override val dependencies: ScriptDependencies? get() = null
        }
    }
}

interface ScriptContents {
    val file: File?
    val annotations: Iterable<Annotation>
    val text: CharSequence?

    @Deprecated("Use DependenciesResolver interface")
    data class Position(val line: Int, val col: Int)
}

data class ScriptReport(val message: String, val severity: Severity = ScriptReport.Severity.ERROR, val position: Position? = null) {
    data class Position(val startLine: Int, val startColumn: Int, val endLine: Int? = null, val endColumn: Int? = null)
    enum class Severity { ERROR, WARNING, INFO, DEBUG }
}

fun ScriptDependencies.asSuccess(): ResolveResult.Success = ResolveResult.Success(this)