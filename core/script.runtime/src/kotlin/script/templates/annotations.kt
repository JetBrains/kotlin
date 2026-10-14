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

package kotlin.script.templates

import kotlin.reflect.KClass
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver

internal const val LEGACY_TEMPLATE_API_DEPRECATION_MESSAGE =
    "Legacy script template API, use kotlin.script.experimental.annotations.KotlinScript instead"

internal const val LEGACY_TEMPLATE_COMPILER_OPTIONS_DEPRECATION_MESSAGE =
    "Legacy script template API, use ScriptCompilationConfiguration.compilerOptions from kotlin.script.experimental.api instead"

@Deprecated(LEGACY_TEMPLATE_API_DEPRECATION_MESSAGE)
const val DEFAULT_SCRIPT_FILE_PATTERN = ".*\\.kts"

@Deprecated(LEGACY_TEMPLATE_API_DEPRECATION_MESSAGE)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScriptTemplateDefinition(
    @Suppress("DEPRECATION", "DEPRECATION_ERROR") val resolver: KClass<out ScriptDependenciesResolver> = DependenciesResolver.NoDependencies::class,
    @Suppress("DEPRECATION") val scriptFilePattern: String = DEFAULT_SCRIPT_FILE_PATTERN
)

@Deprecated("Legacy script template API, use ScriptCompilationConfiguration.refineConfiguration { onAnnotations(...) } from kotlin.script.experimental.api instead")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AcceptedAnnotations(vararg val supportedAnnotationClasses: KClass<out Annotation>)
