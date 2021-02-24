// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

package test

import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class Container<T>

val <X1> X1.notNull get() = typeOf<Container<X1>>()
val <X2> X2.nullable get() = typeOf<Container<X2?>>()

fun box(): String {
    val fqn = className("test.Container")
    assertEquals("$fqn<X1>", "".notNull.toString())
    assertEquals("$fqn<X2?>", "".nullable.toString())
    return "OK"
}

fun className(fqName: String): String {
    val isJS = 1 as Any is Double
    return if (isJS) fqName.substringAfterLast('.') else fqName
}
