/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import org.jdom.Element
import org.jetbrains.kotlin.gradle.KonanArtifactModel
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.io.Serializable

// corresponds to DSL entity:
// macosX64("HelloWorld") {
//   binaries { executable("HelloApp") { ... } }
// }
data class KonanExecutableBase(
    val targetType: KonanTarget,
    val targetName: String,
    val executableName: String
) {
    val name = "$targetName" + if (executableName.isEmpty()) "" else ".$executableName"

    val fullName = "$targetType.$targetName.$executableName"

    fun writeToXml(element: Element) {
        element.setAttribute(XmlExecutable.attributeTargetType, targetType.name)
        element.setAttribute(XmlExecutable.attributeTargetName, targetName)
        element.setAttribute(XmlExecutable.attributeBinaryName, executableName)
    }

    companion object {
        fun readFromXml(element: Element): KonanExecutableBase? {
            val targetName = element.getAttributeValue(XmlExecutable.attributeTargetName) ?: return null
            val executableName = element.getAttributeValue(XmlExecutable.attributeBinaryName) ?: return null
            val target = konanTarget(element.getAttributeValue(XmlExecutable.attributeTargetType)) ?: return null

            return KonanExecutableBase(target, targetName, executableName)
        }

        fun constructFrom(artifact: KonanArtifactModel, targetName: String): KonanExecutableBase? {
            val targetType = konanTarget(artifact.targetPlatform) ?: return null
            return KonanExecutableBase(targetType, targetName, artifact.executableName)
        }

        private fun konanTarget(dslName: String?): KonanTarget? = listOf(
            KonanTarget.LINUX_X64,
            KonanTarget.MACOS_X64,
            KonanTarget.MINGW_X64
        ).firstOrNull { it.name == dslName }
    }
}

class KonanExecutable(
    val base: KonanExecutableBase,
    val executionTargets: List<IdeaKonanExecutionTarget> = ArrayList()
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
            val executionTargets = IdeaKonanExecutionTarget.fromXml(element, base.name, projectDir)

            return KonanExecutable(base, executionTargets)
        }
    }
}