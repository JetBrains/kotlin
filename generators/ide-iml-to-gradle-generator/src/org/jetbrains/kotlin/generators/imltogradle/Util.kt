/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.imltogradle

import org.jdom.Element
import org.jdom.input.SAXBuilder
import org.jetbrains.jps.model.JpsElementFactory
import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.java.JpsJavaDependencyScope
import org.jetbrains.jps.model.java.impl.JpsJavaExtensionServiceImpl
import org.jetbrains.jps.model.module.JpsDependencyElement
import org.jetbrains.jps.model.module.JpsLibraryDependency
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.model.module.JpsModuleDependency
import org.jetbrains.jps.model.serialization.JpsProjectLoader
import java.io.File
import java.net.URL
import java.util.*

fun String.trimMarginWithInterpolations(): String {
    val regex = Regex("""^(\s*\|)(\s*).*$""")
    val out = mutableListOf<String>()
    var prevIndent = ""
    for (line in lines()) {
        val matchResult = regex.matchEntire(line)
        if (matchResult != null) {
            out.add(line.removePrefix(matchResult.groupValues[1]))
            prevIndent = matchResult.groupValues[2]
        } else {
            out.add(prevIndent + line)
        }
    }
    return out.joinToString("\n").trim()
}

fun File.readXml(): Element {
    return inputStream().use { SAXBuilder().build(it).rootElement }
}

suspend fun SequenceScope<Element>.visit(element: Element) {
    element.children.forEach { visit(it) }
    yield(element)
}
fun Element.traverseChildren(): Sequence<Element> {
    return sequence { visit(this@traverseChildren) }
}

inline fun <reified T> Any?.safeAs(): T? {
    return this as? T
}

val JpsDependencyElement.scope: JpsJavaDependencyScope
    get() = JpsJavaExtensionServiceImpl.getInstance().getDependencyExtension(this)?.scope
        ?: error("Cannot get dependency scope for $this")

val JpsDependencyElement.isExported: Boolean
    get() = JpsJavaExtensionServiceImpl.getInstance().getDependencyExtension(this)?.isExported
        ?: error("Cannot get dependency isExported for $this")

fun File.loadJpsProject(): JpsProject {
    val model = JpsElementFactory.getInstance().createModel()
    val project = model.project
    JpsProjectLoader.loadProject(project, mapOf(), this.canonicalPath)
    return project
}

sealed class Either<out A, out B> {
    data class First<out A>(val value: A) : Either<A, Nothing>()
    data class Second<out B>(val value: B) : Either<Nothing, B>()
}

val <T, A : T, B : T> Either<A, B>.value: T
    get() = when (this) {
        is Either.First -> this.value
        is Either.Second -> this.value
    }

inline fun <T> T?.orElse(block: () -> T): T = this ?: block()

val JpsModule.dependencies: List<JpsDependencyElement>
    get() = dependenciesList.dependencies.filter { it is JpsModuleDependency || it is JpsLibraryDependency }

fun fetchJsonsFromBuildserver(ideaMajorVersion: String): List<String> {
    require(ideaMajorVersion.length == 3 && ideaMajorVersion.all { it.isDigit() }) {
        "attachedIntellijVersion='$ideaMajorVersion' must be 3 length all digit string"
    }
    val urlPrefix = jsonUrlPrefixes[ideaMajorVersion] ?: error("'$ideaMajorVersion' platform is absent in mapping")
    return listOf(
        "$urlPrefix/ideaIU-project-structure-mapping.json",
        "$urlPrefix/intellij-core-project-structure-mapping.json"
    ).map { url ->
        try {
            URL(url).readText()
        } catch (ex: Throwable) {
            error("Can't access $url. Is VPN on?")
        }
    }
}

fun File.readProperty(propertyName: String): String {
    return inputStream().use { Properties().apply { load(it) }.getProperty(propertyName) }
        ?: error("Can't find '$propertyName' in '${this.canonicalPath}'")
}
