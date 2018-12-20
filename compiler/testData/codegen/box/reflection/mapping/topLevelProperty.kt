// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

var topLevel = "123"

val fileFacadeClass = object {}::class.java.enclosingClass

fun box(): String {
    val p = ::topLevel

    assert(p.javaField != null) { "Fail p field" }
    val field = p.javaField!!
    assertEquals(fileFacadeClass, field.getDeclaringClass())

    val getter = p.javaGetter!!
    val setter = p.javaSetter!!

    assertEquals(getter, fileFacadeClass.getMethod("getTopLevel"))
    assertEquals(setter, fileFacadeClass.getMethod("setTopLevel", String::class.java))

    assert(getter.invoke(null) == "123") { "Fail k getter" }
    setter.invoke(null, "456")
    assert(getter.invoke(null) == "456") { "Fail k setter" }

    return "OK"
}
