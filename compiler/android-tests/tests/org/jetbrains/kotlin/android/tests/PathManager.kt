/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.android.tests

import java.io.File
import java.io.IOException

class PathManager(destinationDirectory: File, val testDataDirectories: List<File>) {
    private val destinationTestClassesDirectory: File = destinationDirectory.resolve("test-classes")
    private val destinationSrcDirectory: File = destinationDirectory.resolve("src")

    fun prepareDestinationFolder() {
        if (destinationTestClassesDirectory.isDirectory) {
            destinationTestClassesDirectory.listFiles()!!
                .forEach { it.deleteRecursively() }
        } else if (!destinationTestClassesDirectory.mkdirs()) {
            throw IOException("Unable to create directory for test classes: $destinationTestClassesDirectory")
        }

        if (destinationSrcDirectory.isDirectory) {
            destinationSrcDirectory.listFiles()!!
                .filter { it.name != "main" }
                .forEach { it.deleteRecursively() }
        } else if (!destinationSrcDirectory.mkdirs()) {
            throw IOException("Unable to create directory for sources: $destinationSrcDirectory")
        }
    }

    fun prepareFlavorTestClassesDirectory(flavor: String): File {
        val directory = destinationTestClassesDirectory.resolve(flavor)

        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Unable to create test output directory for flavor: $directory")
        }

        return directory
    }

    fun prepareFlavorTestSourceDirectory(flavorName: String): File {
        val directory = destinationSrcDirectory.resolve(
            "androidTest${flavorName.replaceFirstChar(Char::uppercaseChar)}/java/" +
                    CodegenTestsOnAndroidGenerator.TEST_CLASS_PACKAGE.replace(".", "/") + "/"
        )

        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Unable to create source directory for flavor: $directory")
        }

        return directory
    }
}
