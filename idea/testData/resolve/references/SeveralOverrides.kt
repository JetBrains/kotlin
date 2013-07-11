trait A {
    fun foo()
}

trait B {
    fun foo()
}

trait C : A, B {

}

fun test(c: C) {
    c.<caret>foo()
}

// MULTIRESOLVE
// REF: (in A).foo()
// REF: (in B).foo()