// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER

class A {
    companion object {
        fun foo(): Int = 0
    }
}

fun A.foo(): Double = 0.0
fun Any.foo(): Float = 1f

class B {
    fun foo(): String = ""

    companion object {
        fun foo(): Int = 0
    }
}

fun B.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>(): Double = 0.0

fun call(a: Any) {}

fun testA(a: A) {
    call(<!COMPATIBILITY_WARNING!>A::foo<!>)
    call(A.Companion::foo)
}

fun testB(b: B) {
    call(B::foo)
    call(B()::foo)
    call(B.Companion::foo)
}
