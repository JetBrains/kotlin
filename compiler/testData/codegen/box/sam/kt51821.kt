fun interface A {
    fun f(x: String): String
}

fun foo(a: A, vararg s: String): String =
    a.f(s[0])

fun bar(vararg s: String): String =
    foo({ it }, s = s)

fun box(): String = bar("OK")