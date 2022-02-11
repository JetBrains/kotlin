/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.TestWithWorkingDir
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectories
import org.junit.Test
import java.io.File

class RelocatableCachesTest : TestWithWorkingDir() {
    @Test
    fun testLookupStorageAddAllReversedFiles() {
        val originalRoot = workingDir.resolve("original")
        fillLookupStorage(originalRoot, reverseFiles = false, reverseLookups = false)
        val reversedFilesOrderRoot = workingDir.resolve("reversedFiles")
        fillLookupStorage(reversedFilesOrderRoot, reverseFiles = true, reverseLookups = false)
        assertEqualDirectories(originalRoot, reversedFilesOrderRoot, forgiveExtraFiles = false)
    }

    @Test
    fun testLookupStorageAddAllReversedLookups() {
        val originalRoot = workingDir.resolve("original")
        fillLookupStorage(originalRoot, reverseFiles = false, reverseLookups = false)
        val reversedLookupsOrderRoot = workingDir.resolve("reversedLookups")
        fillLookupStorage(reversedLookupsOrderRoot, reverseFiles = false, reverseLookups = true)
        assertEqualDirectories(originalRoot, reversedLookupsOrderRoot, forgiveExtraFiles = false)
    }

    @Test
    fun testLookupStorageAddAllReversedFilesReversedLookups() {
        val originalRoot = workingDir.resolve("original")
        fillLookupStorage(originalRoot, reverseFiles = false, reverseLookups = false)
        val reversedFilesReversedLookupsOrderRoot = workingDir.resolve("reversedFilesReversedLookupsOrderRoot")
        fillLookupStorage(reversedFilesReversedLookupsOrderRoot, reverseFiles = true, reverseLookups = true)
        assertEqualDirectories(originalRoot, reversedFilesReversedLookupsOrderRoot, forgiveExtraFiles = false)
    }

    /**
     * Fills lookup storage in [projectRoot] with N fq-names,
     * where i_th fq-name myscope_i.MyClass_i has lookups for previous fq-names (from 0 to i-1)
     */
    private fun fillLookupStorage(projectRoot: File, reverseFiles: Boolean, reverseLookups: Boolean, storeFullFqNames: Boolean = false) {
        val storageRoot = projectRoot.storageRoot
        val fileToPathConverter = RelativeFileToPathConverter(projectRoot)
        val lookupStorage = LookupStorage(
            storageRoot,
            fileToPathConverter,
            storeFullFqNames = storeFullFqNames
        )
        val files = LinkedHashSet<String>()
        val symbols = LinkedHashSet<LookupSymbol>()
        val lookups = MultiMap.createOrderedSet<LookupSymbol, String>()

        for (i in 0..10) {
            val newSymbol = LookupSymbol(name = "MyClass_$i", scope = "myscope_$i")
            val newSourcePath = projectRoot.resolve("src/${newSymbol.asRelativePath()}").canonicalFile.invariantSeparatorsPath
            symbols.add(newSymbol)

            for (lookedUpSymbol in symbols) {
                lookups.putValue(lookedUpSymbol, newSourcePath)
            }

            files.add(newSourcePath)
        }

        val filesToAdd = if (reverseFiles) files.reversedSet() else files
        val lookupsToAdd = if (reverseLookups) lookups.reversedMultiMap() else lookups
        lookupStorage.addAll(lookupsToAdd, filesToAdd)
        lookupStorage.flush(memoryCachesOnly = false)
    }

    private val File.storageRoot: File
        get() = resolve("storage")

    private fun <K, V> MultiMap<K, V>.reversedMultiMap(): MultiMap<K, V> {
        val newMap = MultiMap.createOrderedSet<K, V>()
        for ((key, values) in entrySet().reversedSet()) {
            newMap.putValues(key, values.reversed())
        }
        return newMap
    }

    private fun <T> Set<T>.reversedSet(): LinkedHashSet<T> =
        reversed().toCollection(LinkedHashSet(size))

    private fun LookupSymbol.asRelativePath(): String =
        if (scope.isBlank()) name else scope.replace('.', '/') + '/' + name
}