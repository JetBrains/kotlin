/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.KotlinVersion
import java.nio.file.Path

public interface BaseToolArguments {
    public sealed class ToolArgument<V>(public val id: String) {
        public class Predefined<V> internal constructor(id: String) : ToolArgument<V>(id)
        public class Custom(id: String) : ToolArgument<String>(id)
    }

    public operator fun <V> get(key: ToolArgument<V>): V?

    public operator fun <V> set(key: ToolArgument<V>, value: V)

    public companion object {
        @JvmField
        public val SUPPRESS_WARNINGS: ToolArgument<Boolean> = ToolArgument.Predefined("SUPPRESS_WARNINGS")

        @JvmField
        public val ALL_WARNINGS_AS_ERRORS: ToolArgument<Boolean> = ToolArgument.Predefined("ALL_WARNINGS_AS_ERRORS")

        @JvmField
        public val ENABLE_EXTRA_WARNINGS: ToolArgument<Boolean> = ToolArgument.Predefined("ENABLE_EXTRA_WARNINGS")

        @JvmField
        public val VERBOSE: ToolArgument<Boolean> = ToolArgument.Predefined("VERBOSE")
    }
}

// should be generated similarly to CommonCompilerArguments
public interface BaseCompilerArguments : BaseToolArguments {
    public class BaseCompilerArgument<V>(public val id: String)

    public operator fun <V> get(key: BaseCompilerArgument<V>): V?

    public operator fun <V> set(key: BaseCompilerArgument<V>, value: V)

    public companion object {
        @JvmField
        public val API_VERSION: BaseCompilerArgument<KotlinVersion> = BaseCompilerArgument("API_VERSION")

        @JvmField
        public val LANGUAGE_VERSION: BaseCompilerArgument<KotlinVersion> = BaseCompilerArgument("LANGUAGE_VERSION")

        @JvmField
        public val PROGRESSIVE: BaseCompilerArgument<Boolean> = BaseCompilerArgument("PROGRESSIVE")

        @JvmField
        public val OPT_IN: BaseCompilerArgument<List<String>> = BaseCompilerArgument("OPT_IN")

        @JvmField
        public val COMPILER_PLUGINS: BaseCompilerArgument<List<CompilerPlugin>> = BaseCompilerArgument("COMPILER_PLUGINS")

        @JvmField
        public val SOURCES: BaseCompilerArgument<List<Path>> = BaseCompilerArgument("SOURCES")
    }
}

public interface BaseKlibCompilerArguments : BaseCompilerArguments {
    public class BaseKlibCompilerArgument<V>(public val id: String)

    public operator fun <V> get(key: BaseKlibCompilerArgument<V>): V?

    public operator fun <V> set(key: BaseKlibCompilerArgument<V>, value: V)

    public companion object {
        @JvmField
        public val PARTIAL_LINKAGE: BaseKlibCompilerArgument<Boolean> = BaseKlibCompilerArgument("PARTIAL_LINKAGE")

        @JvmField
        public val RELATIVE_PATH_BASES: BaseKlibCompilerArgument<List<Path>> = BaseKlibCompilerArgument("RELATIVE_PATH_BASES")

        // ... discuss with the common backend team
    }
}