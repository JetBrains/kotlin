import kotlin.test.assertEquals

fun foo(block: () -> String) = block()

trait A {
    fun f(): String
    fun g(): String
}

fun box(): String {
    val x: A = object : A {
        private inline fun <reified T> localClassName(): String = javaClass<T>().getName()
        override fun f(): String = foo { localClassName<String>() }
        override fun g(): String = foo { localClassName<Int>() }
    }

    assertEquals("java.lang.String", x.f())
    assertEquals("java.lang.Integer", x.g())

    return "OK"
}
