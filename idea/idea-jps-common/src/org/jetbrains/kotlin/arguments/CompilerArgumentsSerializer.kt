// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.arguments

import org.jdom.Element
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.config.restoreNormalOrdering
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File
import kotlin.reflect.KProperty1

interface CompilerArgumentsSerializer<T : CommonToolArguments> {
    val arguments: T
    fun serializeTo(element: Element): Element
}

class CompilerArgumentsSerializerV5<T : CommonToolArguments>(override val arguments: T) : CompilerArgumentsSerializer<T> {

    override fun serializeTo(element: Element): Element = Element(COMPILER_ARGUMENTS_ELEMENT_NAME).apply {
        val newInstance = arguments::class.java.getConstructor().newInstance()
        val flagArgumentsByName = CompilerArgumentsContentProspector.getFlagCompilerArgumentProperties(arguments::class)
            .mapNotNull { prop ->
                prop.safeAs<KProperty1<T, Boolean>>()
                    ?.takeIf { it.get(arguments) != it.get(newInstance) }
                    ?.get(arguments)
                    ?.let { prop.name to it }
            }.toMap()
        saveFlagArguments(this, flagArgumentsByName)

        val stringArgumentsByName = CompilerArgumentsContentProspector.getStringCompilerArgumentProperties(arguments::class)
            .mapNotNull { prop ->
                prop.safeAs<KProperty1<T, String?>>()
                    ?.takeIf { it.get(arguments) != it.get(newInstance) }
                    ?.get(arguments)
                    ?.let { prop.name to it }
            }.toMap()
        saveStringArguments(this, stringArgumentsByName)

        val arrayArgumentsByName = CompilerArgumentsContentProspector.getArrayCompilerArgumentProperties(arguments::class)
            .mapNotNull { prop ->
                prop.safeAs<KProperty1<T, Array<String>?>>()
                    ?.takeIf { it.get(arguments)?.contentEquals(it.get(newInstance)) != true }
                    ?.get(arguments)
                    ?.let { prop.name to it }
            }.filterNot { it.second.isEmpty() }
            .toMap()
        saveArrayArguments(this, arrayArgumentsByName)
        val freeArgs = CompilerArgumentsContentProspector.freeArgsProperty.get(arguments)
        saveElementsList(this, FREE_ARGS_ROOT_ELEMENTS_NAME, FREE_ARGS_ELEMENT_NAME, freeArgs)
        val internalArguments = CompilerArgumentsContentProspector.internalArgumentsProperty.get(arguments).map { it.stringRepresentation }
        saveElementsList(this, INTERNAL_ARGS_ROOT_ELEMENTS_NAME, INTERNAL_ARGS_ELEMENT_NAME, internalArguments)
        restoreNormalOrdering(arguments)
        element.addContent(this)
    }

    companion object {
        private fun saveElementConfigurable(element: Element, rootElementName: String, configurable: Element.() -> Unit) {
            element.addContent(Element(rootElementName).apply { configurable(this) })
        }

        private fun saveStringArguments(element: Element, argumentsByName: Map<String, String>) {
            if (argumentsByName.isEmpty()) return
            saveElementConfigurable(element, STRING_ROOT_ELEMENTS_NAME) {
                argumentsByName.entries.forEach { (name, arg) ->
                    Element(STRING_ELEMENT_NAME).also {
                        it.setAttribute(NAME_ATTR_NAME, name)
                        if (name == "classpath") {
                            saveElementsList(it, ARGS_ATTR_NAME, ARG_ATTR_NAME, arg.split(File.pathSeparator))
                        } else {
                            it.setAttribute(ARG_ATTR_NAME, arg)
                        }
                        addContent(it)
                    }
                }
            }
        }

        private fun saveFlagArguments(element: Element, argumentsByName: Map<String, Boolean>) {
            if (argumentsByName.isEmpty()) return
            saveElementConfigurable(element, FLAG_ROOT_ELEMENTS_NAME) {
                argumentsByName.entries.forEach { (name, arg) ->
                    Element(FLAG_ELEMENT_NAME).also {
                        it.setAttribute(NAME_ATTR_NAME, name)
                        it.setAttribute(ARG_ATTR_NAME, arg.toString())
                        addContent(it)
                    }
                }
            }
        }

        private fun saveElementsList(element: Element, rootElementName: String, elementName: String, elementList: List<String>) {
            if (elementList.isEmpty()) return
            saveElementConfigurable(element, rootElementName) {
                val singleModule = elementList.singleOrNull()
                if (singleModule != null) {
                    addContent(singleModule)
                } else {
                    elementList.forEach { elementValue -> addContent(Element(elementName).also { it.addContent(elementValue) }) }
                }
            }
        }

        private fun saveArrayArguments(element: Element, arrayArgumentsByName: Map<String, Array<String>>) {
            if (arrayArgumentsByName.isEmpty()) return
            saveElementConfigurable(element, ARRAY_ROOT_ELEMENTS_NAME) {
                arrayArgumentsByName.entries.forEach { (name, arg) ->
                    Element(ARRAY_ELEMENT_NAME).also {
                        it.setAttribute(NAME_ATTR_NAME, name)
                        saveElementsList(it, ARGS_ATTR_NAME, ARG_ATTR_NAME, arg.toList())
                        addContent(it)
                    }
                }
            }
        }
    }
}