/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.jetbrains.konan.WorkspaceXML
import org.jdom.Element
import org.jetbrains.kotlin.gradle.KonanArtifactModel
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

// corresponds to DSL entity:
// macosX64("HelloWorld") {
//   binaries { executable("HelloApp") { ... } }
// }
data class KonanExecutableBase(
    val targetType: KonanTarget,
    val targetName: String,
    val executableName: String,
    val projectPrefix: String
) {
    val name = "$targetName" + if (executableName.isEmpty()) "" else ".$executableName"

    val fullName = "$projectPrefix$targetType.$targetName.$executableName"

    fun writeToXml(element: Element) {
        element.setAttribute(WorkspaceXML.Executable.attributeTargetType, targetType.name)
        element.setAttribute(WorkspaceXML.Executable.attributeTargetName, targetName)
        element.setAttribute(WorkspaceXML.Executable.attributeBinaryName, executableName)
        element.setAttribute(WorkspaceXML.Executable.attributeProjectPrefix, projectPrefix)
    }

    companion object {
        fun readFromXml(element: Element): KonanExecutableBase? {
            val targetName = element.getAttributeValue(WorkspaceXML.Executable.attributeTargetName) ?: return null
            val executableName = element.getAttributeValue(WorkspaceXML.Executable.attributeBinaryName) ?: return null
            val target = konanTarget(
                element.getAttributeValue(
                    WorkspaceXML.Executable.attributeTargetType
                )
            ) ?: return null
            val prefix = element.getAttributeValue(WorkspaceXML.Executable.attributeProjectPrefix) ?: ""

            return KonanExecutableBase(target, targetName, executableName, prefix)
        }

        fun constructFrom(artifact: KonanArtifactModel, targetName: String, projectPrefix: String): KonanExecutableBase? {
            val targetType = konanTarget(artifact.targetPlatform) ?: return null
            if (!CompilerOutputKind.valueOf(artifact.type).isExecutable()) return null
            return KonanExecutableBase(targetType, targetName, artifact.executableName, projectPrefix)
        }

        private fun konanTarget(dslName: String?): KonanTarget? = listOf(
            KonanTarget.LINUX_X64,
            KonanTarget.MACOS_X64,
            KonanTarget.MINGW_X64,
            KonanTarget.IOS_X64
        ).firstOrNull { it.name == dslName }

        private fun CompilerOutputKind.isExecutable(): Boolean =
            this == CompilerOutputKind.PROGRAM
    }
}

class KonanExecutable(
    val base: KonanExecutableBase,
    val executionTargets: List<BinaryExecutionTarget> = ArrayList()
) : Comparable<KonanExecutable> {

    override fun compareTo(other: KonanExecutable): Int {
        if (base.fullName == other.base.fullName) return 0
        return if (base.fullName < other.base.fullName) -1 else 1
    }

    fun writeToXml(element: Element, projectDir: File): Element {
        base.writeToXml(element)
        executionTargets.forEach { element.children.add(it.toXml(projectDir)) }

        return element
    }

    companion object {
        fun readFromXml(element: Element, projectDir: File): KonanExecutable? {
            val base = KonanExecutableBase.readFromXml(element) ?: return null
            val executionTargets = BinaryExecutionTarget.fromXml(element, base.name, projectDir)

            return KonanExecutable(base, executionTargets)
        }
    }
}