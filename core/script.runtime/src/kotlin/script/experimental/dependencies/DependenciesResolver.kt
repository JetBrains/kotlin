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

package kotlin.script.experimental.dependencies

import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.LEGACY_DEPENDENCIES_API_DEPRECATION_MESSAGE
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver

@Deprecated(LEGACY_DEPENDENCIES_API_DEPRECATION_MESSAGE)
@Suppress("DEPRECATION", "DEPRECATION_ERROR")
interface DependenciesResolver : ScriptDependenciesResolver {
    fun resolve(scriptContents: ScriptContents, environment: Environment): ResolveResult

    object NoDependencies : DependenciesResolver {
        override fun resolve(scriptContents: ScriptContents, environment: Environment) = ScriptDependencies.Empty.asSuccess()
    }

    sealed class ResolveResult {
        abstract val dependencies: ScriptDependencies?
        abstract val reports: List<ScriptReport>

        data class Success(
            override val dependencies: ScriptDependencies,
            override val reports: List<ScriptReport> = listOf(),
        ) : ResolveResult()

        data class Failure(override val reports: List<ScriptReport>) : ResolveResult() {
            constructor(vararg reports: ScriptReport) : this(reports.asList())

            override val dependencies: ScriptDependencies? get() = null
        }
    }
}

@Deprecated("Legacy script dependencies API, use kotlin.script.experimental.api.ScriptDiagnostic instead")
data class ScriptReport(val message: String, val severity: Severity = Severity.ERROR, val position: Position? = null) {
    data class Position(val startLine: Int, val startColumn: Int, val endLine: Int? = null, val endColumn: Int? = null)
    enum class Severity { FATAL, ERROR, WARNING, INFO, DEBUG }
}

@Deprecated("Legacy script dependencies API, use kotlin.script.experimental.api.ResultWithDiagnostics instead")
@Suppress("DEPRECATION")
fun ScriptDependencies.asSuccess(): DependenciesResolver.ResolveResult.Success = DependenciesResolver.ResolveResult.Success(this)