package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.roots.PackageIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.CollectionQuery
import com.intellij.util.Query

class PackageIndexImpl : PackageIndex() {
    override fun getDirectoriesByPackageName(packageName: String, includeLibrarySources: Boolean): Array<VirtualFile> {
        TODO("Not yet implemented")
    }

    override fun getDirsByPackageName(packageName: String, includeLibrarySources: Boolean): Query<VirtualFile> {
        return CollectionQuery(emptyList())
    }

    override fun getPackageNameByDirectory(dir: VirtualFile): String? {
        TODO("Not yet implemented")
    }

}
