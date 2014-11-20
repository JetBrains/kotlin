import kotlin.test.assertEquals

fun foo(block: () -> String) = block()

inline fun<reified T> bar1(): String = foo() {
    javaClass<T>().getName()
}
inline fun<reified T> bar2(y: String): String = foo() {
    javaClass<T>().getName() + "#" + y
}

inline fun<T1, T2, reified R1, reified R2> bar3(y: String) =
        Pair(bar1<R1>(), bar2<R2>(y))

fun box(): String {
    val x = bar3<Any, Double, Int, String>("OK")

    assertEquals("java.lang.Integer", x.first)
    assertEquals("java.lang.String#OK", x.second)

    return "OK"
}
