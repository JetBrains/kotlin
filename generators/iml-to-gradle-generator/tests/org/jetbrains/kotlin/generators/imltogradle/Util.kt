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
import org.jetbrains.jps.model.library.JpsLibrary
import org.jetbrains.jps.model.module.*
import org.jetbrains.jps.model.serialization.JpsProjectLoader
import org.jetbrains.jps.model.serialization.library.JpsLibraryTableSerializer
import org.jetbrains.jps.model.serialization.module.JpsModuleRootModelSerializer
import java.io.File
import java.util.*
import kotlin.Comparator
import kotlin.collections.HashSet

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

lateinit var projectLevelLibraryNameToJpsLibraryMapping: Map<String, JpsLibrary>
fun JpsLibraryDependency.resolve(moduleImlRootElement: Element?): JpsLibrary? {
    val libraryName = libraryReference.libraryName
    return library ?: moduleImlRootElement?.traverseChildren()
        ?.filter { it.name == JpsModuleRootModelSerializer.LIBRARY_TAG }
        ?.map { JpsLibraryTableSerializer.loadLibrary(it) }
        ?.find { it.name == libraryName }
    ?: projectLevelLibraryNameToJpsLibraryMapping[libraryName]
}

fun Element.traverseChildren(): Sequence<Element> {
    suspend fun SequenceScope<Element>.visit(element: Element) {
        element.children.forEach { visit(it) }
        yield(element)
    }
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
    val project = model.project;
    JpsProjectLoader.loadProject(project, mapOf(), this.canonicalPath)
    return project
}

sealed class Either<out A, out B> {
    data class First<A>(val value: A) : Either<A, Nothing>()
    data class Second<B>(val value: B) : Either<Nothing, B>()
}

inline fun <T : Any> T?.orElse(terminalAction: () -> T): T {
    return this ?: terminalAction()
}

val JpsModule.dependencies: List<JpsDependencyElement>
    get() = dependenciesList.dependencies.filter { it !is JpsSdkDependency && it !is JpsModuleSourceDependency }
