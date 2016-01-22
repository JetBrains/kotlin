package testing

interface I {
    val <caret>p: Int
        get() = 0
}

class A : I

class B : I {
    override val p = 5
}

class C : I

interface II: I
interface III : I {
    override val p: Int get() = 1
}

// REF: (in testing.B).p
// REF: (in testing.III).p


