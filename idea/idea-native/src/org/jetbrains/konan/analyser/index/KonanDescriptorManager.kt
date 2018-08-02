/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.analyser.index

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.vfs.*
import com.intellij.util.containers.ContainerUtil.createConcurrentWeakValueMap
import org.jetbrains.kotlin.backend.konan.library.impl.LibraryReaderImpl
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.metadata.KonanLinkData
import java.util.concurrent.ConcurrentMap

class KonanDescriptorManager : ApplicationComponent {

    companion object {

        private const val currentAbiVersion = 1

        @JvmStatic
        fun getInstance(): KonanDescriptorManager = ApplicationManager.getApplication().getComponent(KonanDescriptorManager::class.java)
    }

    private val descriptorCache = createConcurrentWeakValueMap<VirtualFile, ConcurrentMap<LanguageVersionSettings, ModuleDescriptorImpl>>()
    private val protoCache = createConcurrentWeakValueMap<VirtualFile, KonanLinkData.LinkDataPackageFragment>()

    fun getCachedLibraryDescriptor(virtualFile: VirtualFile, languageVersionSettings: LanguageVersionSettings): ModuleDescriptorImpl {
        return descriptorCache.computeIfAbsent(virtualFile) {
            createConcurrentWeakValueMap()
        }.computeIfAbsent(languageVersionSettings) {
            val reader = LibraryReaderImpl(File(virtualFile.path), currentAbiVersion)
            reader.moduleDescriptor(languageVersionSettings)
        }
    }

    fun getCachedPackageFragment(virtualFile: VirtualFile): KonanLinkData.LinkDataPackageFragment {
        return protoCache.computeIfAbsent(virtualFile) {
            val bytes = virtualFile.contentsToByteArray(false)
            org.jetbrains.kotlin.backend.konan.serialization.parsePackageFragment(bytes)
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
        descriptorCache.remove(virtualFile)
        protoCache.remove(virtualFile)
    }
}
