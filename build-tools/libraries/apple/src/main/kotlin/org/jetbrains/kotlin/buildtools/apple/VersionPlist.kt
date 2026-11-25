/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.w3c.dom.Element
import java.io.InputStream
import java.nio.file.Path
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.inputStream

/**
 * Reads the top-level string, integer and real values of an XML property list, such as Xcode's `version.plist`.
 *
 * Binary plists are not supported. External DTDs are never loaded: plists declare Apple's DTD by URL, and fetching it
 * would reach the network.
 */
internal object VersionPlist {
    class MalformedPlistException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun read(path: Path): Map<String, String> = try {
        path.inputStream().use { read(it) }
    } catch (e: MalformedPlistException) {
        throw MalformedPlistException("$path: ${e.message}", e)
    }

    fun read(input: InputStream): Map<String, String> {
        val document = try {
            newDocumentBuilderFactory().newDocumentBuilder().parse(input)
        } catch (e: org.xml.sax.SAXException) {
            throw MalformedPlistException("not an XML property list: ${e.message}", e)
        }
        val root = document.documentElement
        if (root.tagName != "plist") throw MalformedPlistException("the root element is <${root.tagName}>, not <plist>")
        val dict = root.childElements().firstOrNull()
            ?.takeIf { it.tagName == "dict" }
            ?: throw MalformedPlistException("the top-level value is not a <dict>")

        val values = LinkedHashMap<String, String>()
        val children = dict.childElements()
        var index = 0
        while (index < children.size) {
            val key = children[index]
            if (key.tagName != "key") throw MalformedPlistException("expected <key>, found <${key.tagName}>")
            val value = children.getOrNull(index + 1) ?: throw MalformedPlistException("no value for key '${key.textContent}'")
            if (value.tagName in scalarTags) values[key.textContent] = value.textContent
            index += 2
        }
        return values
    }

    private val scalarTags = setOf("string", "integer", "real")

    private fun Element.childElements(): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    private fun newDocumentBuilderFactory(): DocumentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        isValidating = false
        isExpandEntityReferences = false
        setFeatureIfSupported("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        setFeatureIfSupported("http://xml.org/sax/features/external-general-entities", false)
        setFeatureIfSupported("http://xml.org/sax/features/external-parameter-entities", false)
        setAttributeIfSupported(XMLConstants.ACCESS_EXTERNAL_DTD, "")
        setAttributeIfSupported(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
    }

    private fun DocumentBuilderFactory.setFeatureIfSupported(name: String, value: Boolean) {
        try {
            setFeature(name, value)
        } catch (_: javax.xml.parsers.ParserConfigurationException) {
        }
    }

    private fun DocumentBuilderFactory.setAttributeIfSupported(name: String, value: String) {
        try {
            setAttribute(name, value)
        } catch (_: IllegalArgumentException) {
        }
    }
}
