// p.C
// COMPILATION_ERRORS
// FILE: C.kt
package p

class C : A() {
    override val ap: Int
        get() = super.c

    override fun af(): Int {
        return super.foo()
    }
}

// FILE: A.kt
package p

abstract class A {
    internal val ap: Int = 4
    internal fun af(): Int
}
