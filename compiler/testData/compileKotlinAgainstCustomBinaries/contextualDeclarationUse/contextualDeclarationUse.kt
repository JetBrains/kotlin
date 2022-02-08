package test

fun callOrdinaryAndContextualDeclaration() {
    ordinary()
    with(42) {
        f()
        val a = A()
        a.p
        a.m()
        p
    }
}