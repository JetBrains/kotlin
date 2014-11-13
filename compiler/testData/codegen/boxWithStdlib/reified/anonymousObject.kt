import kotlin.test.assertEquals

abstract class A {
    abstract fun f(): String
}

inline fun<reified T> foo(): A {
    return object : A() {
        override fun f(): String {
            return javaClass<T>().getName()
        }
    }
}

fun box(): String {
    val y = foo<String>();
    assertEquals("java.lang.String", y.f())
    return "OK"
}
