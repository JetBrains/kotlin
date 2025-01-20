/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.JsCompilerArguments.JsCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.EcmascriptVersion
import org.jetbrains.kotlin.buildtools.api.arguments.SourceMapNamesPolicy
import java.nio.file.Path

public interface JsLinkerArguments : BaseToolArguments {
    public class JsLinkerArgument<V> internal constructor(public val id: String)

    public operator fun <V> get(key: JsLinkerArgument<V>): V?

    public operator fun <V> set(key: JsLinkerArgument<V>, value: V)

    public companion object {
        @JvmField
        public val LIBRARIES: JsCompilerArgument<List<Path>> = JsCompilerArgument("LIBRARIES")

        @JvmField
        public val GENERATE_SOURCE_MAP: JsCompilerArgument<Boolean> = JsCompilerArgument("GENERATE_SOURCE_MAP")

        @JvmField
        public val SOURCE_MAP_PREFIX: JsCompilerArgument<Boolean> = JsCompilerArgument("SOURCE_MAP_PREFIX")

        @JvmField
        public val SOURCE_MAP_BASE_DIRS: JsCompilerArgument<List<Path>> = JsCompilerArgument("SOURCE_MAP_BASE_DIRS")

        @JvmField
        public val SOURCE_MAP_EMBED_SOURCES: JsCompilerArgument<Boolean> = JsCompilerArgument("SOURCE_MAP_EMBED_SOURCES")

        @JvmField
        public val SOURCE_MAP_NAMES_POLICY: JsCompilerArgument<SourceMapNamesPolicy> = JsCompilerArgument("SOURCE_MAP_NAMES_POLICY")

        @JvmField
        public val TARGET_ECMASCRIPT_VERSION: JsCompilerArgument<EcmascriptVersion> = JsCompilerArgument("TARGET_ECMASCRIPT_VERSION")

        // ... the set of JS compiler arguments is currently merged with the set of JS linker arguments
        // also there are a lot of experimental arguments
        // We should cooperate with K/JS to define the actual arguments for K/JS
    }
}