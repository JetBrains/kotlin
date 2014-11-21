import kotlin.test.assertEquals

fun foo(block: () -> String) = block()

inline fun <reified T> className(): String = javaClass<T>().getName()

inline fun <reified T> lambdaShouldBeReified(): String = foo { className<T>() }

trait A {
    fun f(): String
    fun g(): String
}
inline fun<reified T1, reified T2> AFactory(): A = object : A {
    override fun f(): String = className<T1>()
    override fun g(): String = foo { className<T2>() }
}

fun box(): String {
    assertEquals("java.lang.String", lambdaShouldBeReified<String>())
    assertEquals("java.lang.Integer", lambdaShouldBeReified<Int>())

    val x: A = AFactory<String, Int>()

    assertEquals("java.lang.String", x.f())
    assertEquals("java.lang.Integer", x.g())

    return "OK"
}
