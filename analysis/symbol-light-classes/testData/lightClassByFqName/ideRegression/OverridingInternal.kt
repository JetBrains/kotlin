// p.C
// COMPILATION_ERRORS
// FILE: C.kt
package p

class C : A(), I {
    override val ap: Int
        get() = super.c

    override fun af(): Int {
        return super.foo()
    }

    override val ip = 5
    override fun if() = 5
}

// FILE: A.kt
package p

abstract class A {
    open internal val ap: Int = 4
    abstract internal fun af(): Int
}

interface I {
    internal val ip: Int
    internal fun if(): Int
}
