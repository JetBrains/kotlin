// LANGUAGE: +ContextParameters
// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

typealias Something1 = Throwable
typealias Something2 = Int

class A {
    context(s: Something1) fun Something2.f(a: String) {}
}

fun box(): String {
    val f = A::class.members.single { it.name == "f" }
    assertEquals(
        "[class test.A, class java.lang.Throwable, int, class java.lang.String]",
        f.parameters.map { it.type.javaType }.toString(),
    )
    return "OK"
}
