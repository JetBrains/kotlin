// FIR_IDENTICAL
interface I {
    val <T> T.id: T
        get() = this
}

class A(i: I) : I by i
