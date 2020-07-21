/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.workspace

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration
import com.jetbrains.konan.KonanBundle
import com.jetbrains.konan.KonanLog
import com.jetbrains.konan.getKotlinNativeVersion
import com.jetbrains.mpp.BinaryExecutable
import com.jetbrains.mpp.workspace.State.readFromXml
import com.jetbrains.mpp.workspace.State.writeToXml
import org.jdom.Element
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.CompilerVersionImpl
import org.jetbrains.kotlin.konan.MetaVersion
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.io.File
import java.util.*
import kotlin.collections.HashSet

// version for which in K/N were added dependencies for debug
private val DEBUG_POSSIBLE = CompilerVersionImpl(MetaVersion.DEV, 1, 3, 60, -1)

abstract class WorkspaceBase(val project: Project) : PersistentStateComponent<Element>, ProjectComponent {
    val allAvailableExecutables = HashSet<BinaryExecutable>()

    abstract val binaryRunConfigurationType: Class<out ConfigurationTypeBase>
    abstract val lldbDriverConfiguration: LLDBDriverConfiguration

    var konanHome: String? = null
        set(value) {
            field = value
            if (value != null) {
                konanVersion = getKotlinNativeVersion(value)
            }
        }
    val lldbHome: File?
        get() {
            if (konanHome == null || !File(konanHome).exists()) {
                KonanLog.LOG.warn("lldbHome requested while konanHome is empty")
                return null
            }

            val propertiesPath = "$konanHome/konan/konan.properties"
            val propertiesFile = File(propertiesPath)
            val hostDependenciesKey = "dependencies.${HostManager.host}"

            if (!propertiesFile.exists()) {
                KonanLog.LOG.error("Kotlin/Native properties file is absent at $propertiesPath")
                return null
            }

            val hostDependencies = Properties().apply { propertiesFile.inputStream().use(::load) }.getProperty(hostDependenciesKey)

            if (hostDependencies == null) {
                KonanLog.LOG.error("No property $hostDependenciesKey at $propertiesPath")
                return null
            }

            val lldbRelative = hostDependencies.split(" ").firstOrNull { it.startsWith("lldb-") }

            if (lldbRelative == null) {
                KonanLog.LOG.error("Property $hostDependenciesKey at $propertiesPath does not specify lldb")
                return null
            }

            return DependencyDirectories.defaultDependenciesRoot.resolve(lldbRelative)
        }

    var konanVersion: CompilerVersion? = null
        set(value) {
            value?.let {
                if (compareCompilerVersion(it, DEBUG_POSSIBLE) < 0) {
                    KonanLog.MESSAGES.createNotification(
                        KonanBundle.message("warning.versionPrior1_3_60", it),
                        NotificationType.WARNING
                    ).notify(project)

                }
            }
            field = value
        }

    val isDebugPossible: Boolean
        get() {
            konanVersion?.let {
                return compareCompilerVersion(it, DEBUG_POSSIBLE) >= 0
            }

            return false
        }

    private fun compareCompilerVersion(lhs: CompilerVersion, rhs: CompilerVersion): Int {
        if (lhs.major != rhs.major) return lhs.major - rhs.major
        if (lhs.minor != rhs.minor) return lhs.minor - rhs.minor
        if (lhs.maintenance != rhs.maintenance) return lhs.maintenance - rhs.maintenance
        return 0
    }

    override fun getState(): Element =
        writeToXml(File(project.basePath!!))

    override fun loadState(stateElement: Element) {
        readFromXml(stateElement, File(project.basePath!!))
    }
}