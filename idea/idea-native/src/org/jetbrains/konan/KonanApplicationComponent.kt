package org.jetbrains.konan

import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.ide.util.TipAndTrickBean
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileTypes.FileTypeManager
import org.jetbrains.kotlin.psi.KtElement

class KonanApplicationComponent : ApplicationComponent {
  override fun getComponentName(): String = "KonanApplicationComponent"

  override fun initComponent() {
    FileTypeManager.getInstance().associateExtension(ArchiveFileType.INSTANCE, "klib")

    val extensionPoint = Extensions.getRootArea().getExtensionPoint(TipAndTrickBean.EP_NAME)
    for (name in arrayOf("Kotlin.html", "Kotlin_project.html", "Kotlin_mix.html", "Kotlin_Java_convert.html")) {
      TipAndTrickBean.findByFileName(name)?.let {
        extensionPoint.unregisterExtension(it)
      }
    }
  }
}
