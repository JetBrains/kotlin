// !CHECK_TYPE

interface A {
    fun <T, E> foo(): E
}

interface B {
    fun <Q, W> foo(): W
}

fun <T> test(x: T) where T : B, T : A {
    x.<!AMBIGUITY!>foo<!><String, Int>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
}
