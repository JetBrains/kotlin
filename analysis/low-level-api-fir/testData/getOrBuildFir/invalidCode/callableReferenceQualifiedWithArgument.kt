class C {
    val lam = { "lam" }
}

fun foo() {
    C::lam(<expr>fun() {}</expr>)
}