// !LANGUAGE: +InlineClasses
// FILE: A.kt
package z

interface IFoo {
    fun foo(): String
}

inline class Z(val s: String) : IFoo {
    constructor(i: Int) : this(i.toString())

    override fun foo(): String = s

    fun bar() = s

    inline fun <T> run(lambda: (String) -> T) = lambda(s)

    companion object {
        fun z(i: Int) = Z(i)
    }
}

// FILE: B.kt
import z.*

fun test(z: Z) {
    if (z.foo() != "1234") throw AssertionError()
    if (z.bar() != "1234") throw AssertionError()
    if (z.run { it } != "1234") throw AssertionError()
    if (listOf(z)[0].s != "1234") throw AssertionError()
}

fun box(): String {
    test(Z("1234"))
    test(Z(1234))
    test(Z.z(1234))
    return "OK"
}