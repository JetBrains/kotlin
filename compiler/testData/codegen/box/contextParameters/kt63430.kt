// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB

abstract class A {
    context(c: C)
    fun P.foo(): String = c.result
}

class B : A() {
    val p = P()

    context(c: C)
    fun test(): String =
        p.run {
            foo()
        }
}

class P
class C(val result: String)

fun box(): String =
    with(C("OK")) {
        B().test()
    }
