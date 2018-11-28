@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

import shadow.org.jdom2.Document
import shadow.org.jdom2.Element
import shadow.org.jdom2.output.Format
import shadow.org.jdom2.output.XMLOutputter

class xml(val name: String, private vararg val args: Pair<String, Any>, block: xml.() -> Unit = {}) {
    private companion object {
        fun makeXml(name: String, vararg args: Pair<String, Any>, block: xml.() -> Unit = {}): xml {
            return xml(name, *args, block = block)
        }
    }

    private val children = mutableListOf<xml>()
    private var value: Any? = null

    init {
        @Suppress("UNUSED_EXPRESSION")
        block()
    }

    fun xml(name: String, vararg args: Pair<String, Any>, block: xml.() -> Unit = {}) {
        children += makeXml(name, *args, block = block)
    }

    fun add(xml: xml) {
        children += xml
    }

    fun raw(text: String) {
        value = text
    }

    private fun toElement(): Element {
        val element = Element(name)

        for (arg in args) {
            element.setAttribute(arg.first, arg.second.toString())
        }

        require(value == null || children.isEmpty())

        value?.let { value ->
            element.addContent(value.toString())
        }

        for (child in children) {
            element.addContent(child.toElement())
        }

        return element
    }

    override fun toString(): String {
        val document = Document().also { it.rootElement = toElement() }
        val output = XMLOutputter().also { it.format = Format.getPrettyFormat() }
        return output.outputString(document)
    }
}