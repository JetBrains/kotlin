@Deprecated("Object")
object Obsolete {
    fun use() {}
}

class Another {
    @Deprecated("Object")
    companion object {
        fun use() {}
    }
}

fun first() {
    <!DEPRECATION!>Another<!>.use()
}

fun useObject() {
    <!DEPRECATION!>Obsolete<!>.use()
    val <!UNUSED_VARIABLE!>x<!> = <!DEPRECATION!>Obsolete<!>
}

fun useCompanion() {
    val <!UNUSED_VARIABLE!>d<!> = <!DEPRECATION!>Another<!>
    val <!UNUSED_VARIABLE!>x<!> = Another.<!DEPRECATION!>Companion<!>
    Another.<!DEPRECATION!>Companion<!>.use()
    <!DEPRECATION!>Another<!>.use()
}
