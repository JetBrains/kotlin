/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.templates

import kotlin.reflect.KClass
import kotlin.script.dependencies.Environment

@Deprecated("temporary workaround for missing functionality, will be replaced by the new API soon")
// Note: all subclasses should provide the same constructor
open class ScriptTemplateAdditionalCompilerArgumentsProvider(val arguments: Iterable<String> = emptyList()) {
    open fun getAdditionalCompilerArguments(@Suppress("UNUSED_PARAMETER") environment: Environment?): Iterable<String> = arguments
}

// Should be deprecated as well, but since we don't have replacement as of yet, leaving it as is
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScriptTemplateAdditionalCompilerArguments(
    val arguments: Array<String> = [],
    val provider: KClass<out ScriptTemplateAdditionalCompilerArgumentsProvider> = ScriptTemplateAdditionalCompilerArgumentsProvider::class
)
