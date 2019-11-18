// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.jvm.*
import kotlin.test.*

var foo = "foo"

class A {
    var bar = "bar"
}

fun box(): String {
    val fooGetter = ::foo.getter.javaMethod ?: return "Fail fooGetter"
    assertEquals("foo", fooGetter.invoke(null))

    val fooSetter = ::foo.setter.javaMethod ?: return "Fail fooSetter"
    fooSetter.invoke(null, "foof")
    assertEquals("foof", foo)

    assertNull(::foo.getter.javaConstructor)
    assertNull(::foo.setter.javaConstructor)


    val a = A()
    val barGetter = A::bar.getter.javaMethod ?: return "Fail barGetter"
    assertEquals("bar", barGetter.invoke(a))

    val barSetter = A::bar.setter.javaMethod ?: return "Fail barSetter"
    barSetter.invoke(a, "barb")
    assertEquals("barb", a.bar)

    assertNull(A::bar.getter.javaConstructor)
    assertNull(A::bar.setter.javaConstructor)

    return "OK"
}
