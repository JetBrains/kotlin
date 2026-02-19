/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.incremental.testingUtils

import com.intellij.openapi.util.io.FileUtil
import java.io.File
import kotlin.math.max

private val COMMANDS = listOf("new", "touch", "delete")
private val COMMANDS_AS_REGEX_PART = COMMANDS.joinToString("|")
private val COMMANDS_AS_MESSAGE_PART = COMMANDS.joinToString("/") { "\".$it\"" }

enum class TouchPolicy {
    TIMESTAMP,
    CHECKSUM
}

/**
 * Optional suffix (first filename extension) that could be inserted before first "regular" extension
 */
enum class OptionalVariantSuffix {
    None,
    K1,
    K2;

    companion object {
        /**
         * A regex used to recognize a variant suffix in the filename.
         * Note: Should match all possible suffixes, but "None", and should be compatible with comparison logic in `genVariantMatchingName`
         */
        val anySuffixRegex = Regex(".(K[12]).")
    }
}

fun String.genVariantMatchingName(expectedOptionalVariant: OptionalVariantSuffix): String? {
    if (expectedOptionalVariant == OptionalVariantSuffix.None) return this
    val variantMatch = OptionalVariantSuffix.anySuffixRegex.find(this) ?: return this
    if (variantMatch.groups[1]?.value != expectedOptionalVariant.name) return null
    return removeRange(variantMatch.range.first, variantMatch.range.last)
}

fun copyTestSources(
    testDataDir: File,
    sourceDestinationDir: File,
    filePrefix: String,
    optionalVariantSuffix: OptionalVariantSuffix = OptionalVariantSuffix.None,
): Map<File, File> {
    val mapping = hashMapOf<File, File>()

    fun copyDir(fromDir: File, toDir: File) {
        FileUtil.ensureExists(toDir)
        for (file in fromDir.listFiles().orEmpty()) {
            if (file.isDirectory) {
                copyDir(file, File(toDir, file.name))
            } else if (file.name.endsWith(".kt") || file.name.endsWith(".java")) {
                val nameWithoutVariant = file.name.genVariantMatchingName(optionalVariantSuffix) ?: continue // prefixed but with different variant
                if (nameWithoutVariant.startsWith(filePrefix)) {
                    val targetFile = File(toDir, nameWithoutVariant.substring(filePrefix.length))
                    if (nameWithoutVariant != file.name /* variant-prefixed file replaces the one without a variant prefix */ ||
                        !mapping.containsKey(targetFile)
                    ) {
                        FileUtil.copy(file, targetFile)
                        mapping[targetFile] = file
                    }
                }
            }
        }
    }

    copyDir(testDataDir, sourceDestinationDir)
    return mapping
}

fun getModificationsToPerform(
    testDataDir: File,
    moduleNames: Collection<String>?,
    allowNoFilesWithSuffixInTestData: Boolean,
    touchPolicy: TouchPolicy,
    optionalVariantSuffix: OptionalVariantSuffix = OptionalVariantSuffix.None,
): List<List<Modification>> {

    fun getModificationsForIteration(newSuffix: String, touchSuffix: String, deleteSuffix: String): List<Modification> {

        fun splitToModuleNameAndFileName(fileName: String): Pair<String?, String> {
            val underscore = fileName.indexOf("_")

            if (underscore != -1) {
                var moduleName = fileName.substring(0, underscore)
                var moduleFileName = fileName.substring(underscore + 1)
                if (moduleName.all { it.isDigit() }) {
                    val (moduleName1, moduleFileName1) = moduleFileName.split("_")
                    moduleName = moduleName1
                    moduleFileName = moduleFileName1
                }

                assert(moduleNames != null) { "File name has module prefix, but multi-module environment is absent" }
                assert(moduleName in moduleNames!!) { "Module not found for file with prefix: $fileName" }

                return Pair(moduleName, moduleFileName)
            }

            assert(moduleNames == null) { "Test is multi-module, but file has no module prefix: $fileName" }
            return Pair(null, fileName)
        }

        val rules = mapOf<String, (String, File) -> Modification>(
            newSuffix to { path, file -> ModifyContent(path, file) },
            touchSuffix to { path, _ -> TouchFile(path, touchPolicy) },
            deleteSuffix to { path, _ -> DeleteFile(path) }
        )

        val modifications = LinkedHashMap<String, Modification>()

        for (file in testDataDir.walkTopDown()) {
            if (!file.isFile) continue

            val (suffix, createModification) = rules.entries.firstOrNull { file.path.endsWith(it.key) } ?: continue

            // NOTE: the code do not allow to combine module prefixes with directory structure
            val relativeFilePath = file.toRelativeString(testDataDir)
            val relativeFilePathWithoutVariant =
                relativeFilePath.genVariantMatchingName(optionalVariantSuffix) ?: continue

            val (moduleName, fileName) = splitToModuleNameAndFileName(relativeFilePathWithoutVariant)
            val srcDir = moduleName?.let { "$it/src" } ?: "src"
            val targetPath = srcDir + "/" + fileName.removeSuffix(suffix)

            if (relativeFilePathWithoutVariant != relativeFilePath /* variant-prefixed file replaces the one without a variant prefix */ ||
                !modifications.containsKey(targetPath)
            ) {
                modifications[targetPath] = createModification(targetPath, file)
            }
        }

        return modifications.values.toList()
    }

    val haveFilesWithoutNumbers = testDataDir.walkTopDown().any { it.name.matches(".+\\.($COMMANDS_AS_REGEX_PART)$".toRegex()) }
    val haveFilesWithNumbers = testDataDir.walkTopDown().any { it.name.matches(".+\\.($COMMANDS_AS_REGEX_PART)\\.\\d+$".toRegex()) }

    if (haveFilesWithoutNumbers && haveFilesWithNumbers) {
        throw IllegalStateException("Bad test data format: files ending with both unnumbered and numbered ${COMMANDS_AS_MESSAGE_PART} were found")
    }
    if (!haveFilesWithoutNumbers && !haveFilesWithNumbers) {
        if (allowNoFilesWithSuffixInTestData) {
            return listOf(listOf())
        }
        else {
            throw IllegalStateException("Bad test data format: no files ending with ${COMMANDS_AS_MESSAGE_PART} found")
        }
    }

    if (haveFilesWithoutNumbers) {
        return listOf(getModificationsForIteration(".new", ".touch", ".delete"))
    }
    else {
        return (1..10)
            .map { getModificationsForIteration(".new.$it", ".touch.$it", ".delete.$it") }
            .filter { it.isNotEmpty() }
    }
}

abstract class Modification(val path: String) {
    abstract fun perform(workDir: File, mapping: MutableMap<File, File>): File?

    override fun toString(): String = "${this::class.java.simpleName} $path"
}

class ModifyContent(path: String, val dataFile: File) : Modification(path) {
    override fun perform(workDir: File, mapping: MutableMap<File, File>): File? {
        val file = File(workDir, path)

        val oldLastModified = file.lastModified()
        file.delete()
        dataFile.copyTo(file)

        val newLastModified = file.lastModified()
        if (newLastModified <= oldLastModified) {
            //Mac OS and some versions of Linux truncate timestamp to nearest second
            file.setLastModified(oldLastModified + 1000)
        }

        mapping[file] = dataFile
        return file
    }
}

class TouchFile(path: String, private val touchPolicy: TouchPolicy) : Modification(path) {
    override fun perform(workDir: File, mapping: MutableMap<File, File>): File? {
        val file = File(workDir, path)

        when (touchPolicy) {
            TouchPolicy.TIMESTAMP -> {
                val oldLastModified = file.lastModified()
                //Mac OS and some versions of Linux truncate timestamp to nearest second
                file.setLastModified(max(System.currentTimeMillis(), oldLastModified + 1000))
            }
            TouchPolicy.CHECKSUM -> {
                file.appendText(" ")
            }
        }

        return file
    }
}

class DeleteFile(path: String) : Modification(path) {
    override fun perform(workDir: File, mapping: MutableMap<File, File>): File? {
        val fileToDelete = File(workDir, path)
        if (!fileToDelete.delete()) {
            throw AssertionError("Couldn't delete $fileToDelete")
        }

        mapping.remove(fileToDelete)
        return null
    }
}
