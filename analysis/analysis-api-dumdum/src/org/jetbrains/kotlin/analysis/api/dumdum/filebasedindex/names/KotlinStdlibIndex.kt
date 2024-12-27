// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import org.jetbrains.kotlin.analysis.api.dumdum.ManifestFileType
import org.jetbrains.kotlin.analysis.api.dumdum.index.FileBasedIndex
import org.jetbrains.kotlin.library.KLIB_MANIFEST_FILE_NAME
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME
import org.jetbrains.kotlin.name.FqName
import java.io.ByteArrayInputStream
import java.util.*
import java.util.jar.Manifest

fun FileBasedIndex.hasSomethingInPackage(indexId: ID<FqName, *>, fqName: FqName, scope: GlobalSearchScope): Boolean {
    return !processValues(indexId, fqName, scope, { _ -> false })
}

class KotlinStdlibIndex : KotlinFileIndexBase() {
    companion object {
        val NAME: ID<FqName, Unit> = ID.create("org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names.KotlinStdlibIndex")

        val KOTLIN_STDLIB_NAME: FqName = FqName("kotlin-stdlib")
        val STANDARD_LIBRARY_DEPENDENCY_NAME: FqName = FqName("kotlin-stdlib-common")

        private const val LIBRARY_NAME_MANIFEST_ATTRIBUTE = "Implementation-Title"
        private const val STDLIB_TAG_MANIFEST_ATTRIBUTE = "Kotlin-Runtime-Component"
    }

    // TODO: refactor [KotlinFileIndexBase] and get rid of FqName here, it's never a proper fully qualified name, just a String wrapper
    private val INDEXER: DataIndexer<FqName, Unit, FileContent> = indexer { fileContent ->
        when {
            fileContent.fileType is ManifestFileType -> {
                val manifest = Manifest(ByteArrayInputStream(fileContent.content))
                val attributes = manifest.mainAttributes
                attributes.getValue(STDLIB_TAG_MANIFEST_ATTRIBUTE) ?: return@indexer null
                val libraryName = attributes.getValue(LIBRARY_NAME_MANIFEST_ATTRIBUTE) ?: return@indexer null
                FqName(libraryName)
            }
            fileContent.fileName == KLIB_MANIFEST_FILE_NAME -> {
                val properties = Properties()
                ByteArrayInputStream(fileContent.content).use { properties.load(it) }
                val libraryName = properties.getValue(KLIB_PROPERTY_UNIQUE_NAME) as? String ?: return@indexer null
                FqName(libraryName)
            }
            else -> null
        }
    }
    override val name: ID<FqName, Unit>
        get() = NAME
    override val version: Int
        get() = 2
    override val inputFilter: List<FileType>
        get() = listOf(ManifestFileType.INSTANCE, PlainTextFileType.INSTANCE)
    override val indexer: DataIndexer<FqName, Unit, FileContent>
        get() = INDEXER
}