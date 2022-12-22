// p.C
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

// COMPILATION_ERRORS