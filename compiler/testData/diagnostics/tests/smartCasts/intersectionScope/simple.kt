interface A {
    fun foo()
}

interface C: A
interface B: A

fun test(c: C) {
    if (c is B) {
        c.foo() // OVERLOAD_RESOLUTION_AMBIGUITY: B.foo() and C.foo()
    }
}
