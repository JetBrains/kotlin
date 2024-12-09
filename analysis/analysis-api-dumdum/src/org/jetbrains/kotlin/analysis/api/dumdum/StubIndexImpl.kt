package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.ObjectStubTree
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.psi.stubs.StubTree
import com.intellij.util.Processor
import com.intellij.util.containers.HashingStrategy
import com.intellij.util.indexing.ID

data class StubValue(
    val stub: StubTree,
    val index: Map<StubIndexKey<*, *>, Map<Any, IntArray>>,
)

val StubIndexValueDescriptor: ValueDescriptor<StubValue> =
    ValueDescriptor("stub", Serializer.dummy())

fun <K> ID<K, *>.asKeyDescriptor(): KeyDescriptor<K> =
    KeyDescriptor(name, Serializer.dummy())

fun interface VirtualFileToDocumentIdMapper {
    fun documentId(virtualFile: VirtualFile): DocumentId<*>
}

fun Index.stubIndex(fileLocator: FileLocator, documentIdMapper: VirtualFileToDocumentIdMapper): StubIndex = let { index ->
    object : StubIndex {
        override fun stub(virtualFile: VirtualFile): ObjectStubTree<*>? =
            index.value(
                documentId = documentIdMapper.documentId(virtualFile),
                valueDescriptor = StubIndexValueDescriptor
            )?.stub

        override fun <K> getContainingFilesIterator(
            indexId: ID<K, *>,
            dataKey: K,
            project: Project,
            scope: GlobalSearchScope,
        ): Iterator<VirtualFile> =
            index
                .documents(
                    IndexKey(
                        (indexId as StubIndexKey<K, *>).asKeyDescriptor(),
                        dataKey
                    )
                )
                .flatMap(fileLocator::locate)
                .filter { scope.contains(it) }
                .iterator()

        override fun <Key, Psi : PsiElement> processElements(
            indexKey: StubIndexKey<Key, Psi>,
            key: Key,
            project: Project,
            scope: GlobalSearchScope,
            requiredClass: Class<Psi>,
            processor: Processor<in Psi>,
        ): Boolean =
            index
                .documents(
                    IndexKey(
                        indexKey.asKeyDescriptor(),
                        key
                    )
                )
                .filter { fileLocator.locate(it).any(scope::contains) }
                .mapNotNull { index.value(it, StubIndexValueDescriptor) }
                .flatMap { stubValue ->
                    stubValue.index[indexKey]?.get(key as Any)?.map { stubId ->
                        @Suppress("UNCHECKED_CAST")
                        stubValue.stub.plainList[stubId].psi as Psi
                    }?.asSequence() ?: emptySequence()
                }
                .all(processor::process)
    }
}

fun stubIndexesUpdate(documentId: DocumentId<*>, tree: StubTree): IndexUpdate<*> {
    val map = tree.indexStubTree { indexKey ->
        HashingStrategy.canonical()
    }
    return IndexUpdate(
        documentId = documentId,
        valueType = StubIndexValueDescriptor,
        value = StubValue(tree, map),
        keys = map.flatMap { (stubIndexKey, stubIndex) ->
            @Suppress("UNCHECKED_CAST")
            val keyDescriptor = stubIndexKey.asKeyDescriptor() as KeyDescriptor<Any>
            stubIndex.keys.map { key ->
                IndexKey(keyDescriptor, key)
            }
        }
    )
}
