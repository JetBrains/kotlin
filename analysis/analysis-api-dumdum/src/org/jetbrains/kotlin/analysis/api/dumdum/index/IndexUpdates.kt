package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubTree
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.util.containers.HashingStrategy
import com.intellij.util.indexing.FileContentImpl

fun buildStub(
    stubSerializerTable: StubSerializersTable,
    file: PsiFile,
): IndexedSerializedStubTree? =
    (file.fileElementType as? IStubFileElementType<*>)?.let { stubFileElementType ->
        val stubElement = stubFileElementType.builder.buildStubTree(file)
        val tree = StubTree(stubElement as PsiFileStub<*>)
        val map = tree.indexStubTree { indexKey ->
            HashingStrategy.canonical()
        }
        IndexedSerializedStubTree(tree.serialize(stubSerializerTable), map)
    }

fun <K, V> buildFileBasedMap(
    project: Project,
    virtualFile: VirtualFile,
    extension: FileBasedIndexExtension<K, V>,
): FileBasedMap<K, V>? =
    if (virtualFile.fileType in extension.inputFilter) {
        val fileContent = FileContentImpl.createByFile(virtualFile, project)
        val map = extension.indexer.map(fileContent)
        FileBasedMap(map.mapValues { (_, v) -> Box(v) })
    } else {
        null
    }

data class FileValues(val map: Map<ValueType<*>, *>)

fun mapFile(
    file: PsiFile,
    stubSerializerTable: StubSerializersTable,
    stubIndexExtensions: StubIndexExtensions,
    fileBasedIndexExtensions: FileBasedIndexExtensions,
): FileValues =
    FileValues(
        buildMap {
            buildStub(stubSerializerTable, file)?.let { stub ->
                put(stubIndexExtensions.indexedSerializedStubTreeType, stub)
            }
            fileBasedIndexExtensions.extensions.forEach { extension ->
                buildFileBasedMap(file.project, file.virtualFile, extension)?.let { fileBasedMap ->
                    put(fileBasedIndexExtensions.mapType(extension.name), fileBasedMap)
                }
            }
        }
    )

fun allValueTypes(
    stubIndexExtensions: StubIndexExtensions,
    fileBasedIndexExtensions: FileBasedIndexExtensions,
): List<ValueType<*>> =
    listOf(stubIndexExtensions.indexedSerializedStubTreeType) +
            fileBasedIndexExtensions.extensions.map { extension ->
                fileBasedIndexExtensions.mapType(extension.name)
            } 
