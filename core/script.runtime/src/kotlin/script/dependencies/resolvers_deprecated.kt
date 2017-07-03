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

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

@Deprecated("Use DependenciesResolver interface")
interface ScriptDependenciesResolver {

    enum class ReportSeverity { ERROR, WARNING, INFO, DEBUG }

    fun resolve(script: ScriptContents,
                environment: Environment?,
                report: (ReportSeverity, String, ScriptContents.Position?) -> Unit,
                previousDependencies: KotlinScriptExternalDependencies?
    ): Future<KotlinScriptExternalDependencies?> = PseudoFuture(null)
}

@Deprecated("Use DependenciesResolver interface")
class BasicScriptDependenciesResolver : ScriptDependenciesResolver

@Deprecated("Use DependenciesResolver interface")
fun KotlinScriptExternalDependencies?.asFuture(): PseudoFuture<KotlinScriptExternalDependencies?> = PseudoFuture(this)

@Deprecated("Use DependenciesResolver interface")
class PseudoFuture<T>(private val value: T): Future<T> {
    override fun get(): T = value
    override fun get(p0: Long, p1: TimeUnit): T  = value
    override fun cancel(p0: Boolean): Boolean = false
    override fun isDone(): Boolean = true
    override fun isCancelled(): Boolean = false
}
