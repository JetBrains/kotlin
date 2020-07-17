/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.update

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.plugins.PluginNode
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.util.isDev
import org.jetbrains.kotlin.idea.util.isEap
import java.io.IOException
import java.net.URL
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.annotation.*

class GooglePluginUpdateVerifier : PluginUpdateVerifier() {
    override val verifierName: String
        get() = KotlinBundle.message("update.name.android.studio")

    // Verifies if a plugin can be installed in Android Studio 3.2+.
    // Currently used only by KotlinPluginUpdater.
    override fun verify(pluginDescriptor: IdeaPluginDescriptor): PluginVerifyResult? {
        if (pluginDescriptor.pluginId.idString != KOTLIN_PLUGIN_ID) {
            return null
        }

        val version = pluginDescriptor.version
        if (isEap(version) || isDev(version)) {
            return PluginVerifyResult.accept()
        }

        try {
            val url = URL(METADATA_FILE_URL)
            val stream = url.openStream()
            val context = JAXBContext.newInstance(PluginCompatibility::class.java)
            val unmarshaller = context.createUnmarshaller()
            val pluginCompatibility = unmarshaller.unmarshal(stream) as PluginCompatibility

            val release = getRelease(pluginCompatibility)
                ?: return PluginVerifyResult.decline(KotlinBundle.message("update.reason.text.no.verified.versions.for.this.build"))

            return if (release.plugins().any { KOTLIN_PLUGIN_ID == it.id && version == it.version })
                PluginVerifyResult.accept()
            else
                PluginVerifyResult.decline(KotlinBundle.message("update.reason.text.version.to.be.verified"))
        } catch (e: Exception) {
            LOG.info("Exception when verifying plugin ${pluginDescriptor.pluginId.idString} version $version", e)
            return when (e) {
                is IOException ->
                    PluginVerifyResult.decline(KotlinBundle.message("update.reason.text.unable.to.connect.to.compatibility.verification.repository"))
                is JAXBException -> PluginVerifyResult.decline(KotlinBundle.message("update.reason.text.unable.to.parse.compatibility.verification.metadata"))
                else -> PluginVerifyResult.decline(
                    KotlinBundle.message("update.reason.text.exception.during.verification",
                        e.message.toString()
                    )
                )
            }
        }
    }

    private fun getRelease(pluginCompatibility: PluginCompatibility): StudioRelease? {
        for (studioRelease in pluginCompatibility.releases()) {
            if (buildInRange(studioRelease.name, studioRelease.sinceBuild, studioRelease.untilBuild)) {
                return studioRelease
            }
        }
        return null
    }

    private fun buildInRange(name: String?, sinceBuild: String?, untilBuild: String?): Boolean {
        val descriptor = PluginNode()
        descriptor.name = name
        descriptor.sinceBuild = sinceBuild
        descriptor.untilBuild = untilBuild
        return PluginManagerCore.isCompatible(descriptor)
    }

    companion object {
        private const val KOTLIN_PLUGIN_ID = "org.jetbrains.kotlin"
        private const val METADATA_FILE_URL = "https://dl.google.com/android/studio/plugins/compatibility.xml"

        private val LOG = Logger.getInstance(GooglePluginUpdateVerifier::class.java)

        private fun PluginCompatibility.releases() = studioRelease ?: emptyArray()
        private fun StudioRelease.plugins() = ideaPlugin ?: emptyArray()

        @XmlRootElement(name = "plugin-compatibility")
        @XmlAccessorType(XmlAccessType.FIELD)
        class PluginCompatibility {
            @XmlElement(name = "studio-release")
            var studioRelease: Array<StudioRelease>? = null

            override fun toString(): String {
                return "PluginCompatibility(studioRelease=${Arrays.toString(studioRelease)})"
            }
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        class StudioRelease {
            @XmlAttribute(name = "until-build")
            var untilBuild: String? = null
            @XmlAttribute(name = "since-build")
            var sinceBuild: String? = null
            @XmlAttribute
            var name: String? = null
            @XmlAttribute
            var channel: String? = null

            @XmlElement(name = "idea-plugin")
            var ideaPlugin: Array<IdeaPlugin>? = null

            override fun toString(): String {
                return "StudioRelease(" +
                        "untilBuild=$untilBuild, name=$name, ideaPlugin=${Arrays.toString(ideaPlugin)}, " +
                        "sinceBuild=$sinceBuild, channel=$channel" +
                        ")"
            }
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        class IdeaPlugin {
            @XmlAttribute
            var id: String? = null
            @XmlAttribute
            var sha256: String? = null
            @XmlAttribute
            var channel: String? = null
            @XmlAttribute
            var version: String? = null

            @XmlElement(name = "idea-version")
            var ideaVersion: IdeaVersion? = null

            override fun toString(): String {
                return "IdeaPlugin(id=$id, sha256=$sha256, ideaVersion=$ideaVersion, channel=$channel, version=$version)"
            }
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        class IdeaVersion {
            @XmlAttribute(name = "until-build")
            var untilBuild: String? = null
            @XmlAttribute(name = "since-build")
            var sinceBuild: String? = null

            override fun toString(): String {
                return "IdeaVersion(untilBuild=$untilBuild, sinceBuild=$sinceBuild)"
            }
        }
    }
}
