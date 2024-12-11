package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.ObjectStubTree
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import com.intellij.util.indexing.ID
import com.intellij.util.io.KeyDescriptor

interface StubIndex {

    fun stub(virtualFile: VirtualFile): ObjectStubTree<*>?

    fun <K> getContainingFilesIterator(
        indexId: ID<K, *>,
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

interface StubIndexExtension<Key, Psi : PsiElement> {
    val key: StubIndexKey<Key, Psi>
    val version: Int
    val keyDescriptor: KeyDescriptor<Key>
}
