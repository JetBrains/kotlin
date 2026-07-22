// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
open class X {
    open fun foo(): Int = 10
}

class Y : X() {
    override fun foo() = C.c
}

object A {
    var a: X = X()
}

object B {
    val b: Int = A.a.foo()
}

object C {
    val c: Int = B.b
}

interface D {
    var x: Int
}

enum class E : D {
    ENTRY {
        override var x: Int = 10
            set(value) {
                C
                field = value
            }
    }
}

fun assign(a: A, d: D) {
    <!REASSIGNED_INSTANCED_STATIC_VAR!>a.a = Y()<!>
    <!POSSIBLY_REASSIGNED_STATIC_VAR_WITH_SETTER!>d.x = 42<!>
}

fun main() {
    <!REASSIGNED_INSTANCED_STATIC_VAR!>A.a = Y()<!>
    <!REASSIGNED_STATIC_VAR_WITH_SETTER!>E.ENTRY.x = A.a.foo()<!>
}
