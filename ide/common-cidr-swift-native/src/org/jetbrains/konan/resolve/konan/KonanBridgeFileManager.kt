package org.jetbrains.konan.resolve.konan

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.konan.resolve.KtModificationCount

class KonanBridgeFileManager(private val project: Project) {
    private val myLock = Object()

    private var myStamp: Long = -1
    private var myActualFiles: MutableMap<KonanTarget, KonanBridgeVirtualFile>? = null

    fun forTarget(target: KonanTarget, name: String = target.run { "$productModuleName/$productModuleName.h" }): KonanBridgeVirtualFile {
        val modificationStamp = KtModificationCount.getInstance(project).get()
        synchronized(myLock) {
            val map = if (myActualFiles == null || myStamp < modificationStamp) {
                val map = hashMapOf<KonanTarget, KonanBridgeVirtualFile>()
                myActualFiles = map
                myStamp = modificationStamp
                map
            } else {
                myActualFiles!!
            }

            map[target]?.let { return it }

            val newFile = KonanBridgeVirtualFile(target, name, project, modificationStamp)
            map[target] = newFile
            return newFile
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KonanBridgeFileManager = project.service()
    }
}