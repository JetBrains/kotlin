/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.arguments.NativeBinary
import org.jetbrains.kotlin.buildtools.api.arguments.NativeTargetPlatform
import java.nio.file.Path

public interface NativeLinkerArguments : BaseToolArguments {
    public class NativeLinkerArgument<V> internal constructor(public val id: String)

    public operator fun <V> get(key: NativeLinkerArgument<V>): V?

    public operator fun <V> set(key: NativeLinkerArgument<V>, value: V)

    public companion object {
        @JvmField
        public val LIBRARIES: NativeLinkerArgument<List<Path>> = NativeLinkerArgument("LIBRARIES")

        @JvmField
        public val GENERATE_TEST_RUNNER: NativeLinkerArgument<Boolean> = NativeLinkerArgument("GENERATE_TEST_RUNNER")

        @JvmField
        public val PRODUCED_BINARY: NativeLinkerArgument<NativeBinary> = NativeLinkerArgument("PRODUCED_BINARY")

        @JvmField
        public val TARGET_PLATFORM: NativeLinkerArgument<NativeTargetPlatform> = NativeLinkerArgument("TARGET_PLATFORM")

        // ... the set of K/N compiler arguments is currently merged with the set of K/N linker arguments
        // We should cooperate with K/N to define the actual arguments for K/N
    }
}