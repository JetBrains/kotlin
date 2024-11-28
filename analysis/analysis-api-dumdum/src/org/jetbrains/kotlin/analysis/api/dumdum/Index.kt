package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.KeyDescriptor

interface StubIndex {
    fun <K, V> getContainingFilesIterator(
        indexId: ID<K, V>,
        dataKey: K,
        project: Project,
        scope: GlobalSearchScope,
    ): Iterator<VirtualFile>

    fun <Key, Psi : PsiElement> processElements(
        indexKey: StubIndexKey<Key, Psi>,
        key: Key,
        project: Project,
        scope: GlobalSearchScope,
        requiredClass: Class<Psi>,
        processor: Processor<in Psi>,
    ): Boolean
}

interface FileBasedIndex {
    fun <K, V> processValues(
        indexId: ID<K, V>,
        dataKey: K,
        filter: GlobalSearchScope,
        processor: Processor<in V>
    ): Boolean
}

interface FileBasedIndexExtension<K, V> {
    val name: ID<K, V>
    val version: Int
    val keyDescriptor: KeyDescriptor<K>
    val valueExternalizer: DataExternalizer<V>

    val inputFilter: List<FileType>

    val indexer: DataIndexer<K, V, FileContent>
}
