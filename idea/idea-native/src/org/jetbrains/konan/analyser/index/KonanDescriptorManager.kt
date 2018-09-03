/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.analyser.index

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.vfs.*
import com.intellij.util.containers.ContainerUtil.createConcurrentWeakValueMap
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.serialization.konan.parsePackageFragment

class KonanDescriptorManager : ApplicationComponent {

    companion object {

        @JvmStatic
        fun getInstance(): KonanDescriptorManager = ApplicationManager.getApplication().getComponent(KonanDescriptorManager::class.java)
    }

    private val protoCache = createConcurrentWeakValueMap<VirtualFile, KonanProtoBuf.LinkDataPackageFragment>()

    fun getCachedPackageFragment(virtualFile: VirtualFile): KonanProtoBuf.LinkDataPackageFragment {
        return protoCache.computeIfAbsent(virtualFile) {
            val bytes = virtualFile.contentsToByteArray(false)
            parsePackageFragment(bytes)
        }
    }

    override fun initComponent() {
        VirtualFileManager.getInstance().addVirtualFileListener(object : VirtualFileListener {
            override fun fileCreated(event: VirtualFileEvent) = invalidateCaches(event.file)
            override fun fileDeleted(event: VirtualFileEvent) = invalidateCaches(event.file)
            override fun fileMoved(event: VirtualFileMoveEvent) = invalidateCaches(event.file)
            override fun contentsChanged(event: VirtualFileEvent) = invalidateCaches(event.file)
            override fun propertyChanged(event: VirtualFilePropertyEvent) = invalidateCaches(event.file)
        })
    }

    private fun invalidateCaches(virtualFile: VirtualFile) {
        protoCache.remove(virtualFile)
    }
}
