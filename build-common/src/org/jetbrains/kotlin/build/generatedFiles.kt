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

package org.jetbrains.kotlin.build

import org.jetbrains.kotlin.incremental.LocalFileKotlinClass
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.jetbrains.kotlin.utils.sure
import java.io.File

open class GeneratedFile<Target>(
        val target: Target,
        val sourceFiles: Collection<File>,
        val outputFile: File
)

class GeneratedJvmClass<Target> (
        target: Target,
        sourceFiles: Collection<File>,
        outputFile: File
) : GeneratedFile<Target>(target, sourceFiles, outputFile) {
    val outputClass = LocalFileKotlinClass.create(outputFile).sure {
        "Couldn't load KotlinClass from $outputFile; it may happen because class doesn't have valid Kotlin annotations"
    }
}

fun File.isModuleMappingFile() = extension == ModuleMapping.MAPPING_FILE_EXT && parentFile.name == "META-INF"
