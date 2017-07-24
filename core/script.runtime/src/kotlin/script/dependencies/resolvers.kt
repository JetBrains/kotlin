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

//discuss

// Provides api to discover dependencies of scripts
// Dependencies can be content-dependent
//
// Some concerns on naming:
// Environment -> ScriptEnvironment (top level class with too common a name)
// ResolveResult -> ResolutionResult
//
// Admittedly DependenciesResolver is too generic a name, but ScriptDependenciesResolver is already taken by deprecated interface
// My guess is script.runtime.jar is not gonna be used as application wide dependency in users project (like stdlib)
// but rather as a dependency for a module that contains DependenciesResolver implementation so maybe this is a non-problem
//
// ResolveResult contains resulting dependencies and reports (diagnostics?)
// reports may contain errors (regardless of result being Success or Failure) which is gonna lead to compiler error and script not run/error highlighting in IDE.
// Is this semantic reasonable? Are Success and Failure misleading names?
// Main idea behind Failure is to be able to distinguish between scenarios where resolver could or could not return meaningful ScriptDependencies object.
// For example, IDE can avoid repainting all external references as errors when Resolver threw an exception or is in inconsistent state.
typealias Environment = Map<String, Any?>

interface DependenciesResolver : @Suppress("DEPRECATION") ScriptDependenciesResolver {
    fun resolve(scriptContents: ScriptContents, environment: Environment): ResolveResult

    object NoDependencies : DependenciesResolver {
        override fun resolve(scriptContents: ScriptContents, environment: Environment) = ScriptDependencies.Empty.asSuccess()
    }

    sealed class ResolveResult {
        abstract val dependencies: ScriptDependencies?
        // reports -> diagnostics
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

// is File a could type to use here?
// No way to get script name if file is not present, should add another property (val fileName: String)
interface ScriptContents {
    val file: File?
    val annotations: Iterable<Annotation>
    val text: CharSequence?

    // nothing to discuss, for compatibility with previous version
    @Deprecated("Use DependenciesResolver interface")
    data class Position(val line: Int, val col: Int)
}

// ScriptReport -> ScriptDiagnostic
data class ScriptReport(val message: String, val severity: Severity = ScriptReport.Severity.ERROR, val position: Position? = null) {
    data class Position(val startLine: Int, val startColumn: Int, val endLine: Int? = null, val endColumn: Int? = null)
    enum class Severity { ERROR, WARNING, INFO, DEBUG }
}

// should we expose this helper?
fun ScriptDependencies.asSuccess(): ResolutionResult.Success = ResolutionResult.Success(this)