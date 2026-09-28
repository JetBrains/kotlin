// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

context(c1: Int, c2: String)
val contextualizedProp: String
    get() = c2 + c1

fun box(): String = context(1, "K") {
    class B {
        val y by ::contextualizedProp
    }
    val property = B::y
    property.isAccessible = true
    val delegate = property.getDelegate(B())
    if (delegate !is KProperty0<*>) return@context "FAIL 1: $delegate"
    if (delegate.get() != "K1") return@context "FAIL 2: ${delegate.get()}"
    if (delegate != ::contextualizedProp) return@context "FAIL 3"
    "OK"
}
