// !CHECK_TYPE

interface A {
    fun foo(): CharSequence?
}

interface B {
    fun foo(): String
}

fun test(c: Any) {
    if (c is B && c is A) {
        <!DEBUG_INFO_SMARTCAST!>c<!>.foo().checkType { _<String>() }
    }
}
