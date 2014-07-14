package org.jetbrains.jet.lang.resolve.android

import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.Attributes
import java.util.HashMap

class AndroidXmlHandler(val elementCallback: (String, String)-> Unit): DefaultHandler() {

    override fun startDocument() {
        super<DefaultHandler>.startDocument()
    }

    override fun endDocument() {
        super<DefaultHandler>.endDocument()
    }

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        val hashMap = attributes.toMap()
        val s = hashMap["id"]
        val idPrefix = "@+id/"
        val className = hashMap["class"] ?: localName
        if (s != null && s.startsWith(idPrefix)) elementCallback(s.replace(idPrefix, ""), className)
    }

    override fun endElement(uri: String?, localName: String, qName: String) {

    }

    public fun Attributes.toMap(): HashMap<String, String> {
        val res = HashMap<String, String>()
        for (index in 0..getLength()-1) {
            val attrName = getLocalName(index)!!
            val attrVal = getValue(index)!!
            res[attrName] = attrVal
        }
        return res
    }
}



