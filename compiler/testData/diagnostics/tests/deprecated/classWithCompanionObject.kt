// FIR_IDENTICAL
class A {
    @Deprecated("deprecated") companion object

    class B
}

val x1 = A.B()

