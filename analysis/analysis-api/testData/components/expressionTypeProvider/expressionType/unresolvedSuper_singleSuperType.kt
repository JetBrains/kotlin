interface A {
    fun foo() {}
}

class B : A {
    fun test() {
        <expr>super</expr>.unresolved()
    }
}