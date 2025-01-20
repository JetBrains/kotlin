/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import java.nio.file.Path

public interface NativeCompilerArguments : BaseKlibCompilerArguments {
    public class NativeCompilerArgument<V> internal constructor(public val id: String)

    public operator fun <V> get(key: NativeCompilerArgument<V>): V?

    public operator fun <V> set(key: NativeCompilerArgument<V>, value: V)

    public companion object {
        @JvmField
        public val LIBRARIES: NativeCompilerArgument<List<Path>> = NativeCompilerArgument("LIBRARIES")

        @JvmField
        public val INCLUDE_BINARIES: NativeCompilerArgument<List<Path>> = NativeCompilerArgument("INCLUDE_BINARIES") // -include-binary

        @JvmField
        public val ENABLE_OPTIMIZATIONS: NativeCompilerArgument<Boolean> = NativeCompilerArgument("ENABLE_OPTIMIZATIONS") // -opt

        @JvmField
        public val OUTPUT_NAME: NativeCompilerArgument<String> = NativeCompilerArgument("OUTPUT_NAME") // -outputName

        @JvmField
        public val PRODUCE_PACKED: NativeCompilerArgument<Boolean> = NativeCompilerArgument("PRODUCE_PACKED") // -nopack

        @JvmField
        public val NO_DEFAULT_LIBS: NativeCompilerArgument<Boolean> = NativeCompilerArgument("NO_DEFAULT_LIBS") // -no-default-libs

        @JvmField
        public val PRODUCE_METADATA_KLIB: NativeCompilerArgument<Boolean> = NativeCompilerArgument("PRODUCE_METADATA_KLIB")

        @JvmField
        public val MANIFEST_FILE: NativeCompilerArgument<Path> = NativeCompilerArgument("MANIFEST_FILE")

        // ... the set of K/N compiler arguments is currently merged with the set of K/N linker arguments
        // We should cooperate with K/N to define the actual arguments for K/N
    }
}