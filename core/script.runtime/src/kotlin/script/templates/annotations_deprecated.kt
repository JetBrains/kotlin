/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.templates

import kotlin.reflect.KClass
import kotlin.script.dependencies.Environment

@Deprecated(LEGACY_TEMPLATE_COMPILER_OPTIONS_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
// Note: all subclasses should provide the same constructor
open class ScriptTemplateAdditionalCompilerArgumentsProvider(val arguments: Iterable<String> = emptyList()) {
    open fun getAdditionalCompilerArguments(@Suppress("UNUSED_PARAMETER", "DEPRECATION") environment: Environment?): Iterable<String> = arguments
}

@Deprecated(LEGACY_TEMPLATE_COMPILER_OPTIONS_DEPRECATION_MESSAGE)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScriptTemplateAdditionalCompilerArguments(
    val arguments: Array<String> = [],
    @Suppress("DEPRECATION_ERROR") val provider: KClass<out ScriptTemplateAdditionalCompilerArgumentsProvider> = ScriptTemplateAdditionalCompilerArgumentsProvider::class
)
