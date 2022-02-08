class A {
    class B {
        class C {
            val x = 10
        }
    }
}

fun x() {
    A.<expr>B</expr>.C().x
}