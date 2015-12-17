// !CHECK_TYPE

interface Common {
    fun foo(): CharSequence?
}

interface A : Common {
    override fun foo(): CharSequence
}

interface B : Common {
    override fun foo(): String
}

fun test(c: Common) {
    if (c is B && c is A) {
        <!DEBUG_INFO_SMARTCAST!>c<!>.foo().checkType { _<String>() }
    }
}
