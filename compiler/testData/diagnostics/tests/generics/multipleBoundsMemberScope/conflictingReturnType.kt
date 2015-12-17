// !CHECK_TYPE

interface A {
    fun foo(): CharSequence
}

interface B {
    fun foo(): String?
}

fun <T> test(x: T) where T : B, T : A {
    x.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>()
}
