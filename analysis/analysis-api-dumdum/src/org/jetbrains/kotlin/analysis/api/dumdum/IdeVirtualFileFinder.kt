package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.ID
import org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names.*
import org.jetbrains.kotlin.analysis.api.dumdum.index.FileBasedIndex
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.io.FileNotFoundException
import java.io.InputStream

class IdeVirtualFileFinder(
    private val scope: GlobalSearchScope,
    private val fileBasedIndex: FileBasedIndex,
) : VirtualFileFinder() {
    override fun findMetadata(classId: ClassId): InputStream? =
        try {
            findVirtualFileWithHeader(classId.asSingleFqName(), KotlinMetadataFileIndex.NAME)
                ?.takeIf { it.exists() }
                ?.inputStream
        } catch (e: FileNotFoundException) {
            null
        }


    override fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String>? = null

    override fun hasMetadataPackage(fqName: FqName): Boolean =
        fileBasedIndex.hasSomethingInPackage(KotlinMetadataFilePackageIndex.NAME, fqName, scope)

    override fun findBuiltInsData(packageFqName: FqName): InputStream? =
        findVirtualFileWithHeader(packageFqName, KotlinBuiltInsMetadataIndex.NAME)?.inputStream

    override fun findSourceOrBinaryVirtualFile(classId: ClassId): VirtualFile? =
        findVirtualFileWithHeader(classId)

    init {
        if (scope != GlobalSearchScope.EMPTY_SCOPE && scope.project == null) {
            LOG.warn("Scope with null project $scope")
        }
    }

    override fun findVirtualFileWithHeader(classId: ClassId): VirtualFile? =
        findVirtualFileWithHeader(classId.asSingleFqName(), KotlinClassFileIndex.NAME)

    private fun findVirtualFileWithHeader(fqName: FqName, key: ID<FqName, *>): VirtualFile? {
        val iterator = fileBasedIndex.getContainingFilesIterator(key, fqName, scope)
        return if (iterator.hasNext()) {
            iterator.next()
        } else {
            null
        }.also {
            println("IdeVirtualFileFinder.findVirtualFileWithHeader($fqName, $key) = $it")
        }
    }

    companion object {
        private val LOG = Logger.getInstance(IdeVirtualFileFinder::class.java)
    }
}