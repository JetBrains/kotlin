// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE, -UNUSED_ANONYMOUS_PARAMETER

inline class Foo(val x: Int)

fun f1(vararg a: Foo) {}
fun f2(vararg a: Foo?) {}

class A {
    fun f3(a0: Int, vararg a1: Foo) {
        fun f4(vararg a: Foo) {}

        val g = fun (vararg v: Foo) {}
    }
}

class B(vararg val s: Foo) {
    constructor(a: Int, vararg s: Foo) : this(*s)
}

annotation class Ann(vararg val f: Foo)