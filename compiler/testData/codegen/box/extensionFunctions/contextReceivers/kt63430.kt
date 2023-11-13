// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

abstract class A {
    context(C)
    fun P.foo(): String = result
}

class B : A() {
    val p = P()

    context(C)
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
