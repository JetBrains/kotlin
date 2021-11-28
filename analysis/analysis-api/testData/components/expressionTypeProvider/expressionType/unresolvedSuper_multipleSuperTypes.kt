interface A {
    fun foo() {}
}

interface B {
    fun bar() {}
}

class C : A, B {
    fun test() {
        <expr>super</expr>.unresolved()
    }
}