/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.workspace

import com.jetbrains.mpp.BinaryExecutable
import com.jetbrains.mpp.RunParameters
import com.jetbrains.mpp.runconfig.BinaryRunConfiguration
import org.jdom.Element
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

internal object State {
    private object XML {
        object Workspace {
            const val state = "state"

            const val konanHome = "konanHome"
            const val allAvailableExecutables = "allAvailableExecutables"
        }

        object BinaryRunConfiguration {
            const val executable = "executable"
            const val variant = "variant"
            const val directory = "directory"
            const val environmentVariables = "environmentVariables"
            const val passPaternalEnvs = "passPaternalEnvs"
            const val parameters = "parameters"
        }

        object BinaryExecutable {
            const val node = "executable"

            const val target = "target"
            const val targetName = "targetName"
            const val execName = "execName"
            const val projectPrefix = "projectPrefix"
            const val isTest = "isTest"

            const val variants = "variants"
        }

        object Variant {
            const val node = "variant"

            const val gradleTask = "gradleTask"
            const val file = "file"
            const val params = "params"
            const val name = "name"
        }

        object RunParameters {
            const val workingDirectory = "workingDirectory"
            const val programParameters = "programParameters"
            const val environmentVariables = "environmentVariables"
        }
    }

    fun WorkspaceBase.writeToXml(projectDir: File): Element {
        val stateElement = Element(XML.Workspace.state)

        konanHome?.let { stateElement.setAttribute(XML.Workspace.konanHome, it) }

        val executablesElement = Element(XML.Workspace.allAvailableExecutables)
        allAvailableExecutables.forEach { exec ->
            val element = Element(XML.BinaryExecutable.node)
            exec.writeToXml(element, projectDir)
            executablesElement.addContent(element)
        }
        stateElement.addContent(executablesElement)

        return stateElement
    }

    fun BinaryRunConfiguration.writeToXml(element: Element, projectDir: File) {
        executable?.let { exec ->
            val executableElement = Element(XML.BinaryRunConfiguration.executable)
            exec.writeToXml(executableElement, projectDir)
            element.addContent(executableElement)
        }
        variant?.let { v ->
            val variantElement = Element(XML.BinaryRunConfiguration.variant)
            v.writeToXml(variantElement, projectDir)
            element.addContent(variantElement)
        }
        workingDirectory?.let {
            element.setAttribute(XML.BinaryRunConfiguration.directory, it)
        }

        val envElement = Element(XML.BinaryRunConfiguration.environmentVariables)
        envs.forEach { (k, v) -> envElement.setAttribute(k, v) }
        element.addContent(envElement)

        element.setAttribute(XML.BinaryRunConfiguration.passPaternalEnvs, isPassParentEnvs.toString())

        programParameters?.let {
            element.setAttribute(XML.BinaryRunConfiguration.parameters, it)
        }
    }

    private fun BinaryExecutable.writeToXml(element: Element, projectDir: File) {
        element.setAttribute(XML.BinaryExecutable.target, target.name)
        element.setAttribute(XML.BinaryExecutable.targetName, targetName)
        element.setAttribute(XML.BinaryExecutable.execName, execName)
        element.setAttribute(XML.BinaryExecutable.projectPrefix, projectPrefix)
        element.setAttribute(XML.BinaryExecutable.isTest, isTest.toString())

        val variantsElement = Element(XML.BinaryExecutable.variants)
        variants.forEach { variant ->
            val e = Element(XML.Variant.node)
            variant.writeToXml(e, projectDir)
            variantsElement.addContent(e)
        }
        element.addContent(variantsElement)
    }

    private fun BinaryExecutable.Variant.writeToXml(element: Element, projectDir: File) {
        element.setAttribute(XML.Variant.name, name)
        element.setAttribute(XML.Variant.gradleTask, gradleTask)
        element.setAttribute(XML.Variant.file, file.toRelativeString(projectDir))

        val paramsElement = Element(XML.Variant.params)
        params.writeToXml(paramsElement)
        element.addContent(paramsElement)
    }

    private fun RunParameters.writeToXml(element: Element) {
        element.setAttribute(XML.RunParameters.workingDirectory, workingDirectory)
        element.setAttribute(XML.RunParameters.programParameters, programParameters)

        val envElement = Element(XML.RunParameters.environmentVariables)
        environmentVariables.forEach { (k, v) ->
            envElement.setAttribute(k, v)
        }
        element.addContent(envElement)
    }

    fun WorkspaceBase.readFromXml(element: Element, projectDir: File) {
        konanHome = element.getAttributeValue(XML.Workspace.konanHome)
        element.getChild(XML.Workspace.allAvailableExecutables)
            ?.getChildren(XML.BinaryExecutable.node)
            ?.map { it.readBinaryExecutableFromXml(projectDir) }
            ?.let { allAvailableExecutables.addAll(it) }
    }

    fun BinaryRunConfiguration.readFromXml(element: Element, projectDir: File) {
        executable = element.getChild(XML.BinaryRunConfiguration.executable)?.readBinaryExecutableFromXml(projectDir)
        variant = element.getChild(XML.BinaryRunConfiguration.variant)?.readVariantFromXml(projectDir)
        workingDirectory = element.getAttributeValue(XML.BinaryRunConfiguration.directory)
        envs = element.getChild(XML.BinaryRunConfiguration.environmentVariables)
            ?.attributes?.associate { it.name to it.value }
            ?: emptyMap()
        isPassParentEnvs = element.getAttributeValue(XML.BinaryRunConfiguration.passPaternalEnvs).toBoolean()
        programParameters = element.getAttributeValue(XML.BinaryRunConfiguration.parameters)
    }

    private fun Element.readBinaryExecutableFromXml(projectDir: File) = BinaryExecutable(
        KonanTarget.predefinedTargets[getAttributeValue(XML.BinaryExecutable.target)] ?: error("Unknown Konan target"),
        getAttributeValue(XML.BinaryExecutable.targetName),
        getAttributeValue(XML.BinaryExecutable.execName),
        getAttributeValue(XML.BinaryExecutable.projectPrefix),
        getAttributeValue(XML.BinaryExecutable.isTest).toBoolean(),
        getChild(XML.BinaryExecutable.variants).getChildren(XML.Variant.node).map { it.readVariantFromXml(projectDir) }
    )

    private fun Element.readVariantFromXml(projectDir: File): BinaryExecutable.Variant {
        val gradleTask = getAttributeValue(XML.Variant.gradleTask)
        val filePath = getAttributeValue(XML.Variant.file)
        val params = getChild(XML.Variant.params).readRunParamsFromXml()
        val name = getAttributeValue(XML.Variant.name)
        return if (gradleTask.contains("debug", true)) {
            BinaryExecutable.Variant.Debug(gradleTask, projectDir.resolve(filePath), params, name)
        } else {
            BinaryExecutable.Variant.Release(gradleTask, projectDir.resolve(filePath), params, name)
        }
    }

    private fun Element.readRunParamsFromXml() = RunParameters(
        getAttributeValue(XML.RunParameters.workingDirectory),
        getAttributeValue(XML.RunParameters.programParameters),
        getChild(XML.RunParameters.environmentVariables).attributes.associate { e -> e.name to e.value }
    )
}