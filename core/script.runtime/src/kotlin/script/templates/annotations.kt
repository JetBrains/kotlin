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
import kotlin.script.dependencies.DependenciesResolver.NoDependencies
import kotlin.script.dependencies.ScriptDependenciesResolver

// discuss

// One of the requests is to allow to share compiler args via script templates: https://youtrack.jetbrains.com/issue/KT-19120
// Making it a property of ScriptTemplateDefinition is a viable options but it doesn't allow to changes compiler arguments based on script content.
// For example, it could be possible to allow file level annotation and use it like:
// @file: ExtraCompilerArgs("-enableWhatever")

const val DEFAULT_SCRIPT_FILE_PATTERN = ".*\\.kts"

// classes annotated with ScriptTemplateDefinition become script templates.
// examples: https://github.com/JetBrains/kotlin/blob/5faad493b4cf7bf33bf82475e966a99a8e835720/compiler/tests/org/jetbrains/kotlin/scripts/ScriptTemplateTest.kt#L564-L601
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScriptTemplateDefinition(val resolver: KClass<out ScriptDependenciesResolver> = NoDependencies::class,
                                          val scriptFilePattern: String = DEFAULT_SCRIPT_FILE_PATTERN)

// usage example: https://github.com/JetBrains/kotlin/blob/88652154c96402475d42ae0496ab0b423cc0a2b2/libraries/tools/kotlin-script-util/src/main/kotlin/org/jetbrains/kotlin/script/util/resolve.kt#L42-L42
//
// ScriptContents::annotations allows access to annotations that are specified on script file via '@file: []'
// AcceptedAnnotations is intended to be put on DependenciesResolver::resolve method and limits what annotations are accessible (none if AcceptedAnnotations is not specified)
// Do we have to limit annotations that are accessible via ScriptContents?
// Seems a good idea since we have to load annotation instances via reflection, which maybe and overhead if resolver doesn't really care
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AcceptedAnnotations(vararg val supportedAnnotationClasses: KClass<out Annotation>)
