/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.util.JUnit4Assertions
import java.io.File

object MockLibraryUtilExt {
    @JvmStatic
    @JvmOverloads
    fun compileJavaFilesLibraryToJar(
        sourcesPath: String,
        jarName: String,
        addSources: Boolean = false,
        extraOptions: List<String> = emptyList(),
        extraClasspath: List<String> = emptyList(),
    ): File {
        return MockLibraryUtil.compileJavaFilesLibraryToJar(
            sourcesPath,
            jarName,
            addSources,
            extraOptions,
            extraClasspath,
            extraModulepath = listOf(),
            JUnit4Assertions
        )
    }

    @JvmStatic
    @JvmOverloads
    fun compileJvmLibraryToJar(
        sourcesPath: String,
        jarName: String,
        addSources: Boolean = false,
        allowKotlinSources: Boolean = true,
        extraOptions: List<String> = emptyList(),
        extraClasspath: List<String> = emptyList(),
        useJava11: Boolean = false,
    ): File {
        return MockLibraryUtil.compileJvmLibraryToJar(
            sourcesPath,
            jarName,
            addSources,
            allowKotlinSources,
            extraOptions,
            extraClasspath,
            extraModulepath = listOf(),
            useJava11,
            JUnit4Assertions
        )
    }

    @JvmStatic
    @JvmOverloads
    fun compileLibraryToJar(
        sourcesPath: String,
        contentDir: File,
        jarName: String,
        addSources: Boolean = false,
        allowKotlinSources: Boolean = true,
        extraOptions: List<String> = emptyList(),
        extraClasspath: List<String> = emptyList(),
        useJava11: Boolean = false
    ): File {
        return MockLibraryUtil.compileLibraryToJar(
            sourcesPath,
            contentDir,
            jarName,
            addSources,
            allowKotlinSources,
            extraOptions,
            extraClasspath,
            extraModulepath = listOf(),
            useJava11,
            JUnit4Assertions
        )
    }

}
