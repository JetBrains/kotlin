// p.C
package p

class C : A() {
    override val ap: Int
        get() = super.c

    override fun af(): Int {
        return super.foo()
    }
}

// LAZINESS:NoLaziness
// FIR_COMPARISON