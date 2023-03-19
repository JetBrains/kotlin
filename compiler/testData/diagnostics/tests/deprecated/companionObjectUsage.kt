// FIR_IDENTICAL
class Another {
    @Deprecated("Object")
    companion object {
        fun use() {}
        const val USE = 42
    }
}

fun first() {
    <!DEPRECATION!>Another<!>.use()
    Another.<!DEPRECATION!>Companion<!>.USE
    <!DEPRECATION!>Another<!>.USE
}

fun useCompanion() {
    val d = <!DEPRECATION!>Another<!>
    val x = Another.<!DEPRECATION!>Companion<!>
    Another.<!DEPRECATION!>Companion<!>.use()
    <!DEPRECATION!>Another<!>.use()
}

@Deprecated("Some")
class Some {
    companion object {
        fun use() {}
    }
}

fun some() {
    <!DEPRECATION!>Some<!>.use()
    <!DEPRECATION!>Some<!>.Companion.use()
}
