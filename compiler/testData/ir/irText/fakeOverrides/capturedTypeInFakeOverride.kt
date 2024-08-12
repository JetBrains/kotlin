// FIR_IDENTICAL
// ISSUE: KT-70395
// IGNORE_BACKEND_K1: JS_IR
// IGNORE_BACKEND_K2: ANY

interface A {
    fun m(x: B<out List<Number>>): Int
}

interface B<T : List<out Number>>

abstract class C : A {
    override fun m(x: B<out List<Number>>): Int = TODO()
}