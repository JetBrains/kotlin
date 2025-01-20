/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.BaseCompilerArguments.BaseCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmTarget
import java.nio.file.Path

// should be generated similarly to K2JVMCompilerArguments
public interface JvmCompilerArguments : BaseCompilerArguments {
    public class JvmCompilerArgument<V>(public val id: String)

    public operator fun <V> get(key: JvmCompilerArgument<V>): V?

    public operator fun <V> set(key: JvmCompilerArgument<V>, value: V)

    public companion object {
        @JvmField
        public val JVM_TARGET: JvmCompilerArgument<JvmTarget> = JvmCompilerArgument("JVM_TARGET")

        @JvmField
        public val DESTINATION: JvmCompilerArgument<Path> = JvmCompilerArgument("DESTINATION")

        @JvmField
        public val CLASSPATH: JvmCompilerArgument<List<Path>> = JvmCompilerArgument("CLASSPATH")

        @JvmField
        public val INCLUDE_RUNTIME: JvmCompilerArgument<Boolean> = JvmCompilerArgument("INCLUDE_RUNTIME")

        @JvmField
        public val JDK_HOME: JvmCompilerArgument<Path> = JvmCompilerArgument("JDK_HOME")

        @JvmField
        public val NO_JDK: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_JDK")

        // ?
        //public val NO_STDLIB: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_STDLIB")
        //public val NO_REFLECT: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_REFLECT")

        // some arguments make little sense for build system integrations,
        // however, would be required for implementing tools like CLI 
        //public val VERSION: CompilerArgument<Boolean> = CompilerArgument("VERSION")

        @JvmField
        public val SCRIPT_TEMPLATES: JvmCompilerArgument<List<String>> = JvmCompilerArgument("SCRIPT_TEMPLATES")

        @JvmField
        public val MODULE_NAME: JvmCompilerArgument<String> = JvmCompilerArgument("MODULE_NAME")

        @JvmField
        public val JAVA_PARAMETERS: JvmCompilerArgument<List<String>> = JvmCompilerArgument("JAVA_PARAMETERS")

        @JvmField
        public val JAVA_SOURCES: BaseCompilerArgument<List<Path>> = BaseCompilerArgument("JAVA_SOURCES")

        @JvmField
        public val KOTLIN_SCRIPT_FILENAME_EXTENSIONS: JvmCompilerArgument<List<String>> =
            JvmCompilerArgument("KOTLIN_SCRIPT_FILENAME_EXTENSIONS")
    }
}