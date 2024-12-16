package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.ObjectStubTree
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import com.intellij.util.indexing.ID

fun interface PsiFileFactory {
    fun psiFile(virtualFile: VirtualFile): PsiFile
}

fun Index.stubIndex(
    stubIndexExtensions: StubIndexExtensions,
    virtualFileFactory: VirtualFileFactory,
    documentIdMapper: FilePathExtractor,
    psiFileFactory: PsiFileFactory,
): StubIndex = let { index ->
    object : StubIndex {
        override fun stub(virtualFile: VirtualFile): ObjectStubTree<*>? =
            index.value(
                fileId = documentIdMapper.filePath(virtualFile),
                valueType = stubIndexExtensions.stubValueType
            )?.stub

        override fun <K> getContainingFilesIterator(
            indexId: ID<K, *>,
            dataKey: K,
            project: Project,
            scope: GlobalSearchScope,
        ): Iterator<VirtualFile> =
            index
                .files(
                    IndexKey(
                        keyType = stubIndexExtensions.keyTypesMap.keyType(indexId),
                        key = dataKey
                    )
                )
                .map(virtualFileFactory::virtualFile)
                .filter(scope::contains)
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
                .files(
                    IndexKey(
                        keyType = stubIndexExtensions.keyTypesMap.keyType(indexKey),
                        key = key
                    )
                )
                .flatMap { fileId ->
                    virtualFileFactory.virtualFile(fileId)
                        .takeIf { scope.contains(it) }
                        ?.let { virtualFile ->
                            index.value(fileId, stubIndexExtensions.stubValueType)?.let { stubValue ->
                                run { // psiFile stub is special and does not compute it's psi element, need to set it manually
                                    val psiFileStub = stubValue.stub.root as PsiFileStubImpl<PsiFile>
                                    if (psiFileStub.psi == null) {
                                        psiFileStub.psi = psiFileFactory.psiFile(virtualFile)
                                    }
                                }
                                stubValue.index[indexKey]?.get(key as Any?)?.map { stubId ->
                                    @Suppress("UNCHECKED_CAST")
                                    stubValue.stub.plainList[stubId].psi as Psi
                                }
                            }
                        }.orEmpty()
                }
                .all(processor::process)
    }
}
