package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.UnloadedModuleDescription
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile

class FileIndexFacadeImpl(project: Project) : FileIndexFacade(project) {
    override fun isInContent(file: VirtualFile): Boolean {
        TODO("Not yet implemented")
    }

    override fun isInSource(file: VirtualFile): Boolean {
        TODO("Not yet implemented")
    }

    override fun isInSourceContent(file: VirtualFile): Boolean {
        TODO("Not yet implemented")
    }

    override fun isInLibrary(file: VirtualFile): Boolean {
        TODO("isInLibrary($file)")
    }

    override fun isInLibraryClasses(file: VirtualFile): Boolean {
        return true
    }

    override fun isInLibrarySource(file: VirtualFile): Boolean {
        TODO("Not yet implemented")
    }

    override fun isExcludedFile(file: VirtualFile): Boolean {
        TODO("Not yet implemented")
    }

    override fun isUnderIgnored(file: VirtualFile): Boolean {
        TODO("Not yet implemented")
    }

    override fun getModuleForFile(file: VirtualFile): Module? {
        return null
    }

    override fun isValidAncestor(baseDir: VirtualFile, child: VirtualFile): Boolean {
        TODO("Not yet implemented")
    }

    override fun getRootModificationTracker(): ModificationTracker {
        TODO("Not yet implemented")
    }

    override fun getUnloadedModuleDescriptions(): MutableCollection<UnloadedModuleDescription> {
        TODO("Not yet implemented")
    }

}
