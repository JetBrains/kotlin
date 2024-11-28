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
    fun <K, V> getValues(
        indexId: ID<K, V>,
        dataKey: K,
        filter: GlobalSearchScope,
    ): List<V>
}

interface FileBasedIndexExtension<K, V> {
    fun getName(): ID<K, V>
    fun getVersion(): Int
    fun getKeyDescriptor(): KeyDescriptor<K>
    fun getValueExternalizer(): DataExternalizer<V>

    fun getInputFilter(): List<FileType>

    fun getIndexer(): DataIndexer<K, V, FileContent>
}
