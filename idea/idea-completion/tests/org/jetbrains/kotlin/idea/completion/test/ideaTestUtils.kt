/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import java.io.File

fun CodeInsightTestFixture.configureWithExtraFile(path: String, vararg extraNameParts: String = arrayOf(".Data")) {
    val fileName = File(path).name

    val noExtensionPath = FileUtil.getNameWithoutExtension(fileName)
    val extensions = arrayOf("kt", "java")
    val extraPaths: List<String> = extraNameParts
            .flatMap { extensions.map { ext -> "$noExtensionPath$it.$ext" } }
            .mapNotNull { File(testDataPath, it).takeIf { it.exists() }?.name }

    configureByFiles(*(listOf(fileName) + extraPaths).toTypedArray())
}

@Suppress("unused") // Used in kotlin-ultimate
inline fun <reified T: Any> Any?.assertInstanceOf() = UsefulTestCase.assertInstanceOf(this, T::class.java)
