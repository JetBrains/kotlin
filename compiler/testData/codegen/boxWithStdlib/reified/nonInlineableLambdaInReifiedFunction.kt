import kotlin.test.assertEquals

fun foo(block: () -> String) = block()
inline fun<reified T : Any> bar1(x: T): String = foo() {
    javaClass<T>().getName()
}
inline fun<reified T : Any> bar2(x: T, y: String): String = foo() {
    javaClass<T>().getName() + "#" + y
}

fun box(): String {

    assertEquals("java.lang.Integer", bar1(1))
    assertEquals("java.lang.String#OK", bar2("abc", "OK"))

    return "OK"
}
