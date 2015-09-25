class C {
    fun f(){}
}

fun foo() {
    val lambda = { -> x(); C() }
    lambda().<caret>f()
}
