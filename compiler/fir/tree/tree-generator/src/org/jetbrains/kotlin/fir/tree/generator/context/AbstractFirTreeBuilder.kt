/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.context

import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.IntermediateBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Type
import org.jetbrains.kotlin.fir.tree.generator.printer.BASE_PACKAGE
import kotlin.reflect.KClass

abstract class AbstractFirTreeBuilder {
    companion object {
        val baseFirElement = Element(
            "Element",
            Element.Kind.Other
        )

        const val string = "String"
        const val boolean = "Boolean"
        const val int = "Int"
    }

    val elements = mutableListOf(baseFirElement)
    val intermediateBuilders = mutableListOf<IntermediateBuilder>()

    protected fun element(name: String, kind: Element.Kind, vararg dependencies: Element, init: Element.() -> Unit = {}): Element =
        Element(name, kind).apply(init).also {
            if (dependencies.isEmpty()) {
                it.parents.add(baseFirElement)
            }
            it.parents.addAll(dependencies)
            elements += it
        }

    protected fun sealedElement(
        name: String,
        kind: Element.Kind,
        vararg dependencies: Element,
        init: Element.() -> Unit = {}
    ): Element {
        return element(name, kind, *dependencies, init = init).apply {
            isSealed = true
        }
    }

    val configurations: MutableMap<Element, () -> Unit> = mutableMapOf()

    fun applyConfigurations() {
        for (element in elements) {
            configurations[element]?.invoke()
        }
    }
}

fun generatedType(type: String): Type = generatedType("", type)

fun generatedType(packageName: String, type: String): Type {
    val realPackage = BASE_PACKAGE + if (packageName.isNotBlank()) ".$packageName" else ""
    return type(realPackage, type, exactPackage = true)
}

fun type(packageName: String?, type: String, exactPackage: Boolean = false): Type {
    val realPackage = if (exactPackage) packageName else packageName?.let { "org.jetbrains.kotlin.$it" }
    return Type(realPackage, type)
}

fun type(type: String): Type = type(null, type)

fun type(klass: KClass<*>): Type {
    val fqn = klass.qualifiedName!!
    val name = klass.simpleName!!
    return type(fqn.dropLast(name.length + 1), name, exactPackage = true)
}
