// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// FULL_JDK

import org.w3c.dom.Element
import org.xml.sax.InputSource
import javax.xml.parsers.DocumentBuilderFactory
import java.io.StringReader

class MyElement(e: Element): Element by e {
    fun bar() = "OK"
}

fun box() : String {
    val factory = DocumentBuilderFactory.newInstance()!!;
    val builder = factory.newDocumentBuilder()!!;
    val source = InputSource(StringReader("<OK></OK>"));
    val doc = builder.parse(source)!!;
    val myElement = MyElement(doc.getDocumentElement()!!)
    return myElement.getTagName()!!
}
