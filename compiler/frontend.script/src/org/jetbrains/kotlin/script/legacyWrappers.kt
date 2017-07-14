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

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.script

import java.io.File
import kotlin.script.dependencies.DependenciesResolver
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptDependencies
import kotlin.script.dependencies.ScriptReport

interface LegacyResolverWrapper

// NOTE: not implementing resolver wrapper since it was not implemented initially and this is deprecated API
@Deprecated("Compatibility with deprecated API")
internal class LegacyPackageDependencyResolverWrapper(
        val legacyResolver: ScriptDependenciesResolver
) : kotlin.script.dependencies.DependenciesResolver, LegacyResolverWrapper {
    override fun resolve(
            scriptContents: kotlin.script.dependencies.ScriptContents,
            environment: Environment
    ): DependenciesResolver.ResolveResult {
        val reports = ArrayList<ScriptReport>()
        val legacyDeps = legacyResolver.resolve(
                object : ScriptContents {
                    override val file: File? get() = scriptContents.file
                    override val annotations: Iterable<Annotation> get() = scriptContents.annotations
                    override val text: CharSequence? get() = scriptContents.text
                },
                environment,
                { sev, msg, pos ->
                    reports.add(ScriptReport(msg, sev.convertSeverity(), pos?.convertPosition()))
                }, null
        ).get() ?: return DependenciesResolver.ResolveResult.Failure(reports)

        val dependencies = ScriptDependencies(
                javaHome = legacyDeps.javaHome?.let(::File),
                classpath = legacyDeps.classpath.toList(),
                imports = legacyDeps.imports.toList(),
                sources = legacyDeps.sources.toList(),
                scripts = legacyDeps.scripts.toList()
        )
        return DependenciesResolver.ResolveResult.Success(dependencies, reports)
    }

    private fun ScriptDependenciesResolver.ReportSeverity.convertSeverity(): ScriptReport.Severity  = when(this) {
        ScriptDependenciesResolver.ReportSeverity.ERROR -> ScriptReport.Severity.ERROR
        ScriptDependenciesResolver.ReportSeverity.WARNING -> ScriptReport.Severity.WARNING
        ScriptDependenciesResolver.ReportSeverity.INFO -> ScriptReport.Severity.INFO
        ScriptDependenciesResolver.ReportSeverity.DEBUG -> ScriptReport.Severity.DEBUG
    }

    private fun ScriptContents.Position.convertPosition(): ScriptReport.Position = ScriptReport.Position(line, col)
}

internal class ApiChangeDependencyResolverWrapper(
        override val delegate: kotlin.script.dependencies.ScriptDependenciesResolver
) : kotlin.script.dependencies.DependenciesResolver,
        DependencyResolverWrapper<kotlin.script.dependencies.ScriptDependenciesResolver>,
        LegacyResolverWrapper {
    override fun resolve(
            scriptContents: kotlin.script.dependencies.ScriptContents,
            environment: Environment
    ): DependenciesResolver.ResolveResult {
        val reports = ArrayList<ScriptReport>()
        val legacyDeps = delegate.resolve(
                scriptContents,
                environment,
                { sev, msg, pos ->
                    reports.add(ScriptReport(msg, sev.convertSeverity(), pos?.convertPosition()))
                }, null
        ).get() ?: return DependenciesResolver.ResolveResult.Failure(reports)

        val dependencies = ScriptDependencies(
                javaHome = legacyDeps.javaHome?.let(::File),
                classpath = legacyDeps.classpath.toList(),
                imports = legacyDeps.imports.toList(),
                sources = legacyDeps.sources.toList(),
                scripts = legacyDeps.scripts.toList()
        )
        return DependenciesResolver.ResolveResult.Success(dependencies, reports)
    }

    private fun kotlin.script.dependencies.ScriptDependenciesResolver.ReportSeverity.convertSeverity(): ScriptReport.Severity = when (this) {
        kotlin.script.dependencies.ScriptDependenciesResolver.ReportSeverity.ERROR -> ScriptReport.Severity.ERROR
        kotlin.script.dependencies.ScriptDependenciesResolver.ReportSeverity.WARNING -> ScriptReport.Severity.WARNING
        kotlin.script.dependencies.ScriptDependenciesResolver.ReportSeverity.INFO -> ScriptReport.Severity.INFO
        kotlin.script.dependencies.ScriptDependenciesResolver.ReportSeverity.DEBUG -> ScriptReport.Severity.DEBUG
    }

    private fun kotlin.script.dependencies.ScriptContents.Position.convertPosition(): ScriptReport.Position = ScriptReport.Position(line, col)
}