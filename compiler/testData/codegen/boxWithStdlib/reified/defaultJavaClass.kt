import kotlin.test.assertEquals

inline fun <reified T> foo(x: Class<T> = javaClass<T>()): String = x.getName()

inline fun <reified R> bar(x: R): String = foo<R>()

fun box(): String {
    assertEquals("java.lang.String", foo<String>())
    assertEquals("java.lang.Integer", foo<Int>())
    assertEquals("java.lang.Object", foo<Any>())

    assertEquals("java.lang.String", bar("abc"))
    assertEquals("java.lang.Integer", bar(1))
    assertEquals("java.lang.Object", bar(Any()))

    return "OK"
}
