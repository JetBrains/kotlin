/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tools

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.fir.ModularizedTestConfig
import org.jetbrains.kotlin.fir.loadModuleDumpFile
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object TestDataExtractor {
    @JvmStatic
    fun main(args: Array<String>) {
        val (destination, rootDir, additionalPaths, models) = processArgs(args)

        val pathsToAdd = linkedSetOf<String>()
        val optionalPathsToAdd = linkedSetOf<String>()

        pathsToAdd.addAll(additionalPaths)

        // TODO: Implement XML processing here using javax.xml.stream if/when needed.
        // This script was simplified to avoid dependencies on com.intellij.openapi.util.JDOMUtil and com.intellij.util.xmlb.
        for (model in models) {
            val moduleData = loadModuleDumpFile(model, ModularizedTestConfig())
            moduleData.forEach {
                pathsToAdd.addAll(it.rawClasspath)
                pathsToAdd.addAll(it.rawSources)
                optionalPathsToAdd.addAll(it.rawFriendDirs)
                pathsToAdd.addAll(it.rawJavaSourceRoots.map { it.path })
                pathsToAdd.add(it.rawOutputDir)
                it.rawJdkHome?.also { pathsToAdd.add(it) } ?: (it.arguments as? K2JVMCompilerArguments)?.jdkHome?.let { pathsToAdd.add(it) }
                it.rawModularJdkRoot?.let { pathsToAdd.add(it) }
            }
        }

        val candidates = collectCandidates(rootDir, pathsToAdd, optionalPathsToAdd)
        zipCandidates(destination, rootDir, candidates)
    }

    private fun collectCandidates(rootDir: File, pathsToAdd: LinkedHashSet<String>, optionalPathsToAdd: LinkedHashSet<String>): List<File> {
        val candidates = mutableListOf<File>()
        var hasErrors = false

        fun resolveFromRoot(pathStr: String): File {
            val relative = if (pathStr.startsWith("/")) pathStr.removePrefix("/") else pathStr
            return rootDir.resolve(relative).absoluteFile
        }

        fun collect(file: File, isRequestedExplicitly: Boolean) {
            if (!file.exists()) {
                if (isRequestedExplicitly) {
                    println("[TestDataExtractor] Missing path: $file")
                    hasErrors = true
                }
                return
            }
            if (!isRequestedExplicitly && file.isHidden) return
            if (file.isDirectory) {
                candidates.add(file)
                val children = file.listFiles()?.toList() ?: emptyList()
                for (child in children) collect(child, isRequestedExplicitly = false)
            } else if (file.isFile) {
                candidates.add(file)
            }
        }

        for (p in pathsToAdd) {
            collect(resolveFromRoot(p), true)
        }
        for (p in optionalPathsToAdd) {
            collect(resolveFromRoot(p), false)
        }

        if (hasErrors) {
            error("[TestDataExtractor] Some files are missing, cannot update the test data zip file")
        }
        println("Candidate files [${candidates.size}]:\n  ${candidates.joinToString("\n  ") { "$it (exists: ${it.exists()})" }} ")
        return candidates
    }

    private fun zipCandidates(destination: File, rootDir: File, candidates: List<File>) {
        // Always write to destination; if it exists, rename it first and read from the renamed file
        val parent = destination.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()

        val oldZip = renameOldZipIfExists(destination, parent)

        // 1) Collect already added zip paths (files and directories) from backup, if present
        val existingPaths = linkedSetOf<String>()
        fun addDirHierarchyToExisting(path: String) {
            // Ensure all parent directories ending with '/'
            var idx = path.lastIndexOf('/')
            while (idx > 0) {
                val dir = path.substring(0, idx + 1)
                existingPaths.add(dir)
                idx = path.lastIndexOf('/', idx - 1)
            }
        }

        if (oldZip?.exists() == true) {
            ZipFile(oldZip).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val e = entries.nextElement()
                    existingPaths.add(e.name)
                    if (e.isDirectory) addDirHierarchyToExisting(e.name)
                }
            }
        }

        // Sort candidates lexicographically by their zip path
        fun toZipPath(file: File): String {
            val rel = file.absoluteFile.toPath().normalize().let { abs ->
                rootDir.absoluteFile.toPath().normalize().relativize(abs)
            }.toString().replace('\\', '/')
            return if (file.isDirectory) (if (rel.endsWith("/")) rel else "${'$'}rel/") else rel
        }

        val sortedCandidates = candidates.sortedBy { toZipPath(it) }

        // Open destination for writing in all cases
        ZipOutputStream(BufferedOutputStream(FileOutputStream(destination))).use { zos ->
            // Copy existing entries first (if any) from backup
            if (oldZip != null && oldZip.exists()) {
                ZipFile(oldZip).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val e = entries.nextElement()
                        val newEntry = ZipEntry(e.name)
                        println("Copying existing entry: ${e.name}")
                        // Preserve timestamps to avoid unnecessary diffs
                        newEntry.time = e.time
                        zos.putNextEntry(newEntry)
                        if (!e.isDirectory) {
                            zip.getInputStream(e).use { it.copyTo(zos) }
                        }
                        zos.closeEntry()
                    }
                }
            }

            fun ensureParentDirs(path: String) {
                var idx = path.lastIndexOf('/')
                while (idx > 0) {
                    val dirPath = path.substring(0, idx + 1)
                    if (existingPaths.add(dirPath)) {
                        zos.putNextEntry(ZipEntry(dirPath))
                        zos.closeEntry()
                    }
                    idx = path.lastIndexOf('/', idx - 1)
                }
            }

            for (file in sortedCandidates) {
                val entryName = toZipPath(file)
                if (existingPaths.contains(entryName)) continue
                println("Adding new entry: $entryName")
                if (file.isDirectory) {
                    if (existingPaths.add(entryName)) {
                        zos.putNextEntry(ZipEntry(entryName))
                        zos.closeEntry()
                    }
                } else {
                    ensureParentDirs(entryName)
                    val entry = ZipEntry(entryName)
                    entry.time = file.lastModified()
                    zos.putNextEntry(entry)
                    FileInputStream(file).use { it.copyTo(zos) }
                    zos.closeEntry()
                    existingPaths.add(entryName)
                }
            }

            zos.flush()
        }

        // Remove backup after successful write
        oldZip?.let { if (it.exists()) it.delete() }
    }

    private fun renameOldZipIfExists(destination: File, parent: File?): File? {
        if (destination.exists()) {
            val baseName = destination.name
            var renamedFile = File(parent, "$baseName.old")
            var idx = 1
            while (renamedFile.exists()) {
                renamedFile = File(parent, "$baseName.old.$idx")
                idx++
            }
            val renamed = destination.renameTo(renamedFile)
            if (!renamed) {
                // Fallback: copy to backup and delete original
                FileInputStream(destination).use { input ->
                    FileOutputStream(renamedFile).use { output -> input.copyTo(output) }
                }
                destination.delete()
            }
            return renamedFile
        }
        return null
    }

    private fun processArgs(args: Array<String>): ExtractorArgs {
        // Simple arguments parser
        var destination: File? = null
        var rootDir: File? = null
        val additionalPaths = mutableListOf<String>()
        val models = mutableListOf<File>()

        var argIdx = 0
        while (argIdx < args.size) {
            val arg = args[argIdx++]
            when (arg) {
                "-d" -> destination = File(args[argIdx++])
                "-r" -> rootDir = File(args[argIdx++])
                "-a" -> additionalPaths += args[argIdx++]
                "-a@" -> {
                    File(args[argIdx++]).forEachLine {
                        if (it.isNotBlank()) {
                            val filePath = it.trim()
                            additionalPaths += filePath
                        }
                    }
                }
                else -> {
                    val f = File(arg)
                    if (f.isFile && f.exists() && f.extension == "xml") {
                        println("Single model file: $arg")
                        models += f
                    } else if (f.isDirectory) {
                        println("Model directory: $arg")
                        f.walkTopDown().filter { it.extension == "xml" }.forEach { models += it }
                    } else error("Unknown or unrecognised model file or directory: $arg")
                }
            }
        }

        if (destination == null) error("Destination (-d) argument is not specified")
        if (rootDir == null) error("Root directory (-r) argument is not specified")
        println("Root directory: ${rootDir.absolutePath}")

        println("Destination: ${destination.normalize().absolutePath} (already exists: ${destination.exists()})")
        if (additionalPaths.isEmpty() && models.isEmpty()) error("No models and no additional files specified, nothing to do")
        return ExtractorArgs(destination, rootDir.absoluteFile, additionalPaths, models)
    }

    private data class ExtractorArgs(
        val destination: File,
        val rootDir: File,
        val additionalPaths: List<String>,
        val models: List<File>
    )
}