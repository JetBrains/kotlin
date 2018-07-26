package org.jetbrains.konan.analyser.index

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.stubs.StubTree
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.backend.konan.library.impl.LibraryReaderImpl
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.metadata.KonanLinkData
import java.util.concurrent.ConcurrentMap

private const val currentAbiVersion = 1

class KonanDescriptorManager : ApplicationComponent {
  companion object {
    @JvmStatic
    val INSTANCE: KonanDescriptorManager
      get() = ApplicationManager.getApplication().getComponent(KonanDescriptorManager::class.java)
  }

  override fun initComponent() {
    VirtualFileManager.getInstance().addVirtualFileListener(object : VirtualFileListener {
      override fun fileCreated(event: VirtualFileEvent) {
        val file = event.file

        descriptorCache.values.forEach { it.remove(file) }
        protoCache.remove(file)
        stubCache.remove(file)
      }
    })
  }

  private val descriptorCache = ContainerUtil.createConcurrentSoftValueMap<LanguageVersionSettings, ConcurrentMap<VirtualFile, ModuleDescriptorImpl>>()
  private val protoCache = ContainerUtil.createConcurrentSoftValueMap<VirtualFile, KonanLinkData.LinkDataPackageFragment>()
  private val stubCache = ContainerUtil.createConcurrentSoftValueMap<VirtualFile, StubTree>()

  fun getDescriptor(file: VirtualFile, specifics: LanguageVersionSettings): ModuleDescriptorImpl {
    val cache = descriptorCache.computeIfAbsent(specifics) {
      ContainerUtil.createConcurrentSoftValueMap()
    }

    return cache.computeIfAbsent(file) {
      val reader = LibraryReaderImpl(File(file.path), currentAbiVersion)
      reader.moduleDescriptor(specifics)
    }
  }

  fun parsePackageFragment(file: VirtualFile): KonanLinkData.LinkDataPackageFragment {
    return protoCache.computeIfAbsent(file) {
      val bytes = file.contentsToByteArray(false)
      org.jetbrains.kotlin.backend.konan.serialization.parsePackageFragment(bytes)
    }
  }

  fun getStub(file: VirtualFile): StubTree? {
    return stubCache[file]
  }

  fun cacheStub(file: VirtualFile, stubTree: StubTree?) {
    stubCache[file] = stubTree
  }
}