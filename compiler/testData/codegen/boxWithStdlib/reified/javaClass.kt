import kotlin.test.assertEquals

inline fun<reified T> javaClassName(): String {
    return javaClass<T>().getName()
}

fun box(): String {
    assertEquals("java.lang.String", javaClassName<String>())
    assertEquals("java.lang.Integer", javaClassName<Int>())
    assertEquals("java.lang.Object", javaClassName<Any>())
    return "OK"
}
