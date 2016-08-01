/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.script

import java.io.File
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

const val DEFAULT_SCRIPT_FILE_PATTERN = ".*\\.kts"

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScriptTemplateDefinition(val resolver: KClass<out ScriptDependenciesResolver> = BasicScriptDependenciesResolver::class,
                                          val scriptFilePattern: String = DEFAULT_SCRIPT_FILE_PATTERN)

interface ScriptContents {

    data class Position(val line: Int, val col: Int)

    val file: File?
    val annotations: Iterable<Annotation>
    val text: CharSequence?
}

class PseudoFuture<T>(private val value: T): Future<T> {
    override fun get(): T = value
    override fun get(p0: Long, p1: TimeUnit): T  = value
    override fun cancel(p0: Boolean): Boolean = false
    override fun isDone(): Boolean = true
    override fun isCancelled(): Boolean = false
}

fun KotlinScriptExternalDependencies?.asFuture(): PseudoFuture<KotlinScriptExternalDependencies?> = PseudoFuture(this)

interface ScriptDependenciesResolver {

    enum class ReportSeverity { ERROR, WARNING, INFO, DEBUG }

    fun resolve(script: ScriptContents,
                environment: Map<String, Any?>?,
                report: (ReportSeverity, String, ScriptContents.Position?) -> Unit,
                previousDependencies: KotlinScriptExternalDependencies?
    ): Future<KotlinScriptExternalDependencies?> = PseudoFuture(null)
}

@Suppress("unused") // used in gradle-script-kotlin for a moment
@Deprecated("Use new ScriptDependenciesResolver, this one is left for temporary compatibility of new resolvers to kotlin plugin 1.1-M01",
            ReplaceWith("ScriptDependenciesResolver"))
interface ScriptDependenciesResolverEx {
    fun resolve(script: ScriptContents,
                environment: Map<String, Any?>?,
                previousDependencies: KotlinScriptExternalDependencies?
    ): KotlinScriptExternalDependencies? = null
}

class BasicScriptDependenciesResolver : ScriptDependenciesResolver

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AcceptedAnnotations(vararg val supportedAnnotationClasses: KClass<out Annotation>)

internal fun sameSignature(left: KFunction<*>, right: KFunction<*>): Boolean =
        left.parameters.size == right.parameters.size &&
        left.parameters.zip(right.parameters).all {
            it.first.kind == KParameter.Kind.INSTANCE ||
            it.first.type == it.second.type
        }

