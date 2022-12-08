// TARGET_BACKEND: JVM
// IGNORE_INLINER: IR

// WITH_STDLIB

import kotlin.test.assertEquals

fun foo(block: () -> String) = block()

inline fun<reified T : Any> bar1(): String = foo() {
    T::class.java.getName()
}
inline fun<reified T : Any> bar2(y: String): String = foo() {
    T::class.java.getName() + "#" + y
}

inline fun<T1, T2, reified R1 : Any, reified R2 : Any> bar3(y: String) =
        Pair(bar1<R1>(), bar2<R2>(y))

fun box(): String {
    val x = bar3<Any, Double, Int, String>("OK")

    assertEquals("java.lang.Integer", x.first)
    assertEquals("java.lang.String#OK", x.second)

    return "OK"
}
