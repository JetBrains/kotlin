interface A {
    fun <T, E> foo(): E
}

interface B {
    fun <Q, W> foo(): Q
}

fun test(c: Any) {
    if (c is B && c is A) {
        c.<!AMBIGUITY!>foo<!><String, Int>()
    }
}
