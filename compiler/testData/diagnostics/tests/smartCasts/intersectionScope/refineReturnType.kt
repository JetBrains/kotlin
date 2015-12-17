// !CHECK_TYPE

interface A {
    fun foo(): CharSequence?
}

interface B : A {
    override fun foo(): String
}

fun test(a: A) {
    if (a is B) {
        <!DEBUG_INFO_SMARTCAST!>a<!>.foo()
        <!DEBUG_INFO_SMARTCAST!>a<!>.foo().checkType { _<String>() }
    }
}
