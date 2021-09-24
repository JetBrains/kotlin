// FIR_IDENTICAL
object O {
    // tailrec since `O` is a singleton
    tailrec fun foo(i: Int): Int = if (i < 0) 0 else O.foo(i - 1)
}

class A {
    <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo(i: Int) = if (i < 0) 0 else A.foo(i - 1)

    companion object {
        fun foo(i: Int) = 42 + i
    }
}

class B {
    <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo(i: Int) = if (i < 0) 0 else O.foo(i - 1)
}
