/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.modules

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.ClassIdToJavaClass
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo

/**
 * Reads the [JavaModuleInfo] of a `module-info.java` from its text, resolving the module annotations lazily through
 * `findClassOnClasspath`. Returns `null` if the text declares no module.
 *
 * `cli-base` has no Java source parser: a front-end which has one provides its reader through
 * [putJavaModuleInfoSourceReader]; `cli-jvm` does it in `configureJavaModulesContentRoots`. If a Java source root contains a
 * `module-info.java` and no reader is configured, the classpath resolution reports an error.
 */
fun interface JavaModuleInfoSourceReader {
    fun read(text: CharSequence, findClassOnClasspath: ClassIdToJavaClass): JavaModuleInfo?
}

// Hand-written rather than generated into `JVMConfigurationKeys`: `config.jvm` cannot see `JavaModuleInfo` and `ClassIdToJavaClass`.
private val JAVA_MODULE_INFO_SOURCE_READER: CompilerConfigurationKey<JavaModuleInfoSourceReader> =
    CompilerConfigurationKey.create("module-info.java reader")

val CompilerConfiguration.javaModuleInfoSourceReader: JavaModuleInfoSourceReader?
    get() = get(JAVA_MODULE_INFO_SOURCE_READER)

fun CompilerConfiguration.putJavaModuleInfoSourceReader(reader: JavaModuleInfoSourceReader) {
    put(JAVA_MODULE_INFO_SOURCE_READER, reader)
}
