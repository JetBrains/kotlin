// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// FULL_JDK
// WITH_RUNTIME

//This front-end problem test added to box ones only cause of FULL_JDK support
import org.w3c.dom.Element

class MyElement(e: Element): Element by e {
    fun bar() = "OK"
}

fun box() : String {
    val touch = MyElement::class.java
    return "OK"
}
