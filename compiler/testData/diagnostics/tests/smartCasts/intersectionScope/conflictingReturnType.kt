// !CHECK_TYPE

interface A {
    fun foo(): CharSequence
}

interface B {
    fun foo(): String?
}

fun test(c: Any) {
    if (c is B && c is A) {
        c.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>()
    }
}
