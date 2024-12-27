package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.dumdum.index.FileBasedIndex
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory


class IdeVirtualFileFinderFactory(val fileBasedIndex: FileBasedIndex) : VirtualFileFinderFactory {
    override fun create(scope: GlobalSearchScope): VirtualFileFinder =
        IdeVirtualFileFinder(scope, fileBasedIndex)

    override fun create(project: Project, module: ModuleDescriptor): VirtualFileFinder =
        IdeVirtualFileFinder(GlobalSearchScope.allScope(project), fileBasedIndex)
}