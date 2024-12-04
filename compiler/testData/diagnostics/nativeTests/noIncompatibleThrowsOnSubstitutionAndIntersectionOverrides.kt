// FIR_IDENTICAL
// ISSUE: KT-68094

import kotlin.Throws

interface I {
    @Throws(Throwable::class)
    fun f()
}

abstract class B<T>: I {
    override fun f() { }
}

open class C: B<Int>(), I { }

class D: C(), I {
    override fun f() { }
}