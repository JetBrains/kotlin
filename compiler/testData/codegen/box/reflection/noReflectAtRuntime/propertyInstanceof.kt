// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

import kotlin.reflect.*
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class A {
    val readonly: String = ""
    var mutable: String = ""
}

val readonly: String = ""
var mutable: String = ""

fun box(): String {
    assertTrue(::readonly is KProperty0<*>)
    assertFalse(::readonly is KMutableProperty0<*>)
    assertFalse(::readonly is KProperty1<*, *>)
    assertFalse(::readonly is KProperty2<*, *, *>)

    assertTrue(::mutable is KProperty0<*>)
    assertTrue(::mutable is KMutableProperty0<*>)
    assertFalse(::mutable is KProperty1<*, *>)
    assertFalse(::mutable is KProperty2<*, *, *>)

    assertFalse(A::readonly is KProperty0<*>)
    assertTrue(A::readonly is KProperty1<*, *>)
    assertFalse(A::readonly is KMutableProperty1<*, *>)
    assertFalse(A::readonly is KProperty2<*, *, *>)

    assertFalse(A::mutable is KProperty0<*>)
    assertTrue(A::mutable is KProperty1<*, *>)
    assertTrue(A::mutable is KMutableProperty1<*, *>)
    assertFalse(A::mutable is KProperty2<*, *, *>)

    return "OK"
}
