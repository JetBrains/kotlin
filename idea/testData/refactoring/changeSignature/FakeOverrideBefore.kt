trait A {
    fun f() {}
}

trait B {
    fun f() {}
}

trait C : A, B {

}

fun usage(c: C) {
    c.<caret>f()
}