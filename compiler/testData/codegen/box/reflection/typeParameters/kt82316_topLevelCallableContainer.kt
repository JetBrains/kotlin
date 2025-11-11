// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.test.assertEquals

fun <T> f(t: T) {}
var <U> U.p: Unit
    get() = Unit
    set(value) {}

fun check(c: KCallable<*>) {
    val t1 = c.typeParameters.single()
    val t2 = c.parameters.single().type.classifier as KTypeParameter
    assertEquals(t1, t2)
}

fun box(): String {
    val f: KFunction1<String, Unit> = ::f
    check(f)

    check(String::p)

    return "OK"
}
