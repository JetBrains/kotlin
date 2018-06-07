/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.update

import com.intellij.ide.plugins.IdeaPluginDescriptor
import org.intellij.lang.annotations.Language
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.bind.annotation.*
import kotlin.collections.HashSet

class GooglePluginUpdateVerifier : PluginUpdateVerifier() {
    override fun verify(pluginDescriptor: IdeaPluginDescriptor): PluginVerifyResult? {
        if (pluginDescriptor.pluginId.idString != "org.jetbrains.kotlin") {
            return null
        }

        // val approvedKotlinVersions = fetchApprovedKotlinVersions()
        // if (pluginDescriptor.version !in approvedKotlinVersions) {
        //    // TODO: Modify message
        //    return PluginVerifyResult.decline("Plugin is waiting for Google approve.")
        // }

        return PluginVerifyResult.accept()
    }

    private fun fetchApprovedKotlinVersions(): Set<String> {
        // TODO: Choose the host and do an actual request
        // TODO: Fetching the full list each time might be time-consuming
        // TODO: Process parsing and fetching errors

        // TODO: Emulate long operation to check progress indicators. Remove after actual implementation is ready.
        Thread.sleep(200)

        @Language("XML")
        val answer = """
            <plugin-compatibility>
              <studio-release name="3.2 Canary" channel="" since-build="181.1" until-build="181.*">
                <idea-plugin id="org.jetbrains.kotlin" version="1.2.41-release-Studio3.2c12-3" channel="stable" sha256="">
                  <idea-version since-build="181.3007.1" until-build="181.*"/>
                </idea-plugin>
                <idea-plugin id="org.jetbrains.kotlin" version="1.2.50-eap-17-Studio3.2-1" channel="eap-1.2" sha256="">
                  <idea-version since-build="181.3007.1" until-build="181.*"/>
               </idea-plugin>
              </studio-release>
              <studio-release name="3.1.2" channel="" since-build="173.4" until-build="173.*">
                <idea-plugin id="org.jetbrains.kotlin" version="1.2.41-release-Studio3.1-1" channel="stable" sha256="">
                  <idea-version since-build="173.1" until-build="173.4301.25"/>
                </idea-plugin>
              </studio-release>
            </plugin-compatibility>
        """.trimIndent()

        val context = JAXBContext.newInstance(PluginCompatibility::class.java)
        val unmarshaller = context.createUnmarshaller()
        val pluginCompatibility = unmarshaller.unmarshal(answer.byteInputStream()) as PluginCompatibility

        val approvedKotlinVersions = HashSet<String>()
        for (studioRelease in pluginCompatibility.releases()) {
            for (ideaPlugin in studioRelease.plugins()) {
                if (ideaPlugin.id == KOTLIN_PLUGIN_ID) {
                    val version = ideaPlugin.version
                    if (version != null) {
                        approvedKotlinVersions.add(version)
                    }
                }
            }
        }

        return approvedKotlinVersions
    }

    companion object {
        private const val KOTLIN_PLUGIN_ID = "org.jetbrains.kotlin"

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

