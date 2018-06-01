package org.jetbrains.kotlin.idea.versionCheck

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginNode
import com.intellij.ide.plugins.RepositoryHelper
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.PermanentInstallationID
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.util.io.HttpRequests
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.PluginUpdateStatus
import java.net.URLEncoder

class IdeaPluginVersionChecker : PluginVersionChecker {
  override fun getLatest(currentVersion: String?, host: String?): IdeaPluginDescriptor? {
    if (host == null) {
      val buildNumber = ApplicationInfo.getInstance().apiVersion
      val os = URLEncoder.encode(SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION, CharsetToolkit.UTF8)
      val uid = PermanentInstallationID.get()
      return getLatestFromMainRepository(buildNumber, os, uid, currentVersion)
    }
    else return getLatestFromCustomRepository(host, currentVersion)
  }

  private fun getLatestFromMainRepository(buildNumber: String,
                                          os: String,
                                          uid: String,
                                          currentVersion: String?): IdeaPluginDescriptor? {
    val pluginId = KotlinPluginUtil.KOTLIN_PLUGIN_ID.idString
    val url = "https://plugins.jetbrains.com/plugins/list?pluginId=$pluginId&build=$buildNumber&pluginVersion=$currentVersion&os=$os&uuid=$uid"
    val responseDoc = HttpRequests.request(url).connect {
      JDOMUtil.load(it.inputStream)
    }
    if (responseDoc.name != "plugin-repository") {
      throw PluginVersionCheckFailed("Unexpected plugin repository response: ${JDOMUtil.writeElement(responseDoc, "\n")}")
    }
    if (responseDoc.children.isEmpty()) {
      // No plugin version compatible with current IDEA build; don't retry updates
      return null;
    }
    val newVersion = responseDoc.getChild("category")?.getChild("idea-plugin")?.getChild("version")?.text
    if (newVersion == null) {
      throw PluginVersionCheckFailed("Couldn't find plugin version in repository response: ${JDOMUtil.writeElement(responseDoc, "\n")}")
    }
    return initPluginDescriptor(newVersion)
  }

  private fun getLatestFromCustomRepository(host: String, currentVersion: String?): IdeaPluginDescriptor? {
    val plugins = try {
      RepositoryHelper.loadPlugins(host, null)
    }
    catch (e: Exception) {
      throw PluginVersionCheckFailed("Checking custom plugin repository $host failed with $e")
    }
    return plugins.find { it.pluginId == KotlinPluginUtil.KOTLIN_PLUGIN_ID }
  }

  private fun initPluginDescriptor(newVersion: String): IdeaPluginDescriptor {
    val originalPlugin = PluginManager.getPlugin(KotlinPluginUtil.KOTLIN_PLUGIN_ID)!!
    return PluginNode(KotlinPluginUtil.KOTLIN_PLUGIN_ID).apply {
      version = newVersion
      name = originalPlugin.name
      description = originalPlugin.description
    }
  }
}