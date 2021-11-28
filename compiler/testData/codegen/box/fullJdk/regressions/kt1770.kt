// TARGET_BACKEND: JVM

// FULL_JDK
// WITH_STDLIB

//This front-end problem test added to box ones only cause of FULL_JDK support
import org.w3c.dom.Element

class MyElement(e: Element): Element by e {
    fun bar() = "OK"
}

fun box() : String {
    val touch = MyElement::class.java
    return "OK"
}
