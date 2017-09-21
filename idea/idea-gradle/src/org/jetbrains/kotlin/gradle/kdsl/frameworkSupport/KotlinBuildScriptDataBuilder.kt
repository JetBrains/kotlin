/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.gradle.kdsl.frameworkSupport

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil

class KotlinBuildScriptDataBuilder(buildScriptFile: VirtualFile) : BuildScriptDataBuilder(buildScriptFile) {
    private val plugins: MutableSet<String> = ContainerUtil.newTreeSet<String>()
    private val properties: MutableSet<String> = ContainerUtil.newTreeSet<String>()
    private val repositories: MutableSet<String> = ContainerUtil.newTreeSet<String>()
    private val dependencies: MutableSet<String> = ContainerUtil.newTreeSet<String>()

    private val buildScriptProperties: MutableSet<String> = ContainerUtil.newTreeSet<String>()
    private val buildScriptRepositories: MutableSet<String> = ContainerUtil.newTreeSet<String>()
    private val buildScriptDependencies: MutableSet<String> = ContainerUtil.newTreeSet<String>()
    private val other: MutableSet<String> = ContainerUtil.newTreeSet<String>()

    override fun addPluginDefinition(definition: String): BuildScriptDataBuilder = apply { plugins.add(definition) }

    override fun addRepositoriesDefinition(definition: String): BuildScriptDataBuilder = apply { repositories.add(definition) }

    override fun addPropertyDefinition(definition: String): BuildScriptDataBuilder = apply { properties.add(definition) }

    override fun addDependencyNotation(notation: String): BuildScriptDataBuilder = apply { dependencies.add(notation) }

    override fun addBuildscriptPropertyDefinition(definition: String): BuildScriptDataBuilder = apply { buildScriptProperties.add(definition) }

    override fun addBuildscriptRepositoriesDefinition(definition: String): BuildScriptDataBuilder = apply { buildScriptRepositories.add(definition) }

    override fun addBuildscriptDependencyNotation(notation: String): BuildScriptDataBuilder = apply { buildScriptDependencies.add(notation) }

    override fun addOther(definition: String): BuildScriptDataBuilder = apply { other.add(definition) }

    override fun buildMainPart(): String = buildString {

        appendlnIfNotNull(buildBuildScriptBlock())

        appendlnIfNotNull(buildBlock("apply", plugins))

        if (properties.isNotEmpty()) {
            properties.forEach { appendln(it) }
            appendln()
        }

        appendlnIfNotNull(buildBlock("repositories", repositories))

        appendlnIfNotNull(buildBlock("dependencies", dependencies))

        other.forEach { appendln(it) }
    }

    private fun buildBuildScriptBlock(): String? = buildString {
        if (buildScriptProperties.isEmpty() || buildScriptRepositories.isEmpty() || buildScriptDependencies.isEmpty()) {
            return null
        }

        appendln("buildscript {")
        buildScriptProperties.forEach { appendln(it.withMargin) }
        appendln()
        appendlnIfNotNull(buildBlock("repositories", buildScriptRepositories)?.withMargin)
        appendlnIfNotNull(buildBlock("dependencies", buildScriptDependencies)?.withMargin)
        appendln("}")
    }

    private fun buildBlock(name: String, lines: Set<String>): String? = buildString {
        if (lines.isEmpty()) {
            return null
        }

        appendln("$name {")
        lines.forEach { appendln(it.withMargin) }
        appendln("}")
    }

    private val String.withMargin: String
        get() = lines().joinToString(separator = "\n") { "    " + it }

    private fun StringBuilder.appendlnIfNotNull(text: String?) = text?.let { appendln(it) }
}
