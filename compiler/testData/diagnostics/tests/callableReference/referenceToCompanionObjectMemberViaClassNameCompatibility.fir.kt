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

fun B.foo(): Double = 0.0

fun call(a: Any) {}

fun testA(a: A) {
    call(A::foo)
    call(A.Companion::foo)
}

fun testB(b: B) {
    call(B::foo)
    call(B()::foo)
    call(B.Companion::foo)
}
