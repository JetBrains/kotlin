// LAMBDAS: CLASS
// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

// WITH_REFLECT

import kotlin.test.assertEquals

fun <T> bar(): String {
    return { t: T -> t }.toString()
}

class Baz<T, V> {
    fun <V : T> baz(v: V): String {
        return (fun(t: List<T>): V = v).toString()
    }
}

open class Foo<T, U : List<T>>(val lambda: (T) -> U)
class Bar<T> : Foo<T, List<T>>({ listOf(it) })

fun box(): String {
    assertEquals("(T) -> T", bar<String>())
    assertEquals("(kotlin.collections.List<T>) -> V", Baz<String, Int>().baz<String>(""))
    assertEquals("(T) -> kotlin.collections.List<T>", Bar<Int>().lambda.toString())
    return "OK"
}
