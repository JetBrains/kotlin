trait G

class A {
    class object A {

    }
}

trait B {
    class object : G {

    }
}

fun main() {
    A
    A.A
    g(B.Default)
    g(B)

    A.ext()
    A.A.ext()
}

fun g(g: G) { <!UNUSED_EXPRESSION!>g<!> }

fun A.A.ext() {
}