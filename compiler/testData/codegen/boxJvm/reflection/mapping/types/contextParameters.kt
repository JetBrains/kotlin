// LANGUAGE: +ContextParameters
// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.reflect.jvm.javaType
import kotlin.reflect.KMutableProperty
import kotlin.test.assertEquals

typealias Something1 = Throwable
typealias Something2 = Int

class A {
    context(s: Something1) fun Something2.f(a: String) {}

    context(s: Something1) var Something2.p: Long
        get() = 0L
        set(value) {}
}

fun box(): String {
    val f = A::class.members.single { it.name == "f" }
    assertEquals(
        "[class test.A, class java.lang.Throwable, int, class java.lang.String]",
        f.parameters.map { it.type.javaType }.toString(),
    )

    val p = A::class.members.single { it.name == "p" } as KMutableProperty<*>
    assertEquals(
        "[class test.A, class java.lang.Throwable, int]",
        p.parameters.map { it.type.javaType }.toString(),
    )
    assertEquals(
        "[class test.A, class java.lang.Throwable, int]",
        p.getter.parameters.map { it.type.javaType }.toString(),
    )
    assertEquals(
        "[class test.A, class java.lang.Throwable, int, long]",
        p.setter.parameters.map { it.type.javaType }.toString(),
    )
    return "OK"
}
