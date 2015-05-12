interface A {
    fun f() {}
}

interface B {
    fun f() {}
}

interface C : A, B {

}

fun usage(c: C) {
    c.<caret>f()
}