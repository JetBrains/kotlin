class A {
    class B {
        class C {
            val x = 10
        }
    }
}

fun x() {
    <expr>A.B</expr>.C().x
}