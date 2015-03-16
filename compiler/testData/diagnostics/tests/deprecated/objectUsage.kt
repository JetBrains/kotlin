deprecated("Object")
object Obsolete {
    fun use() {}
}

class Another {
    deprecated("Object")
    companion object {
        fun use() {}
    }
}

fun first() {
    <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Another<!>.use()
}

fun useObject() {
    <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>.use()
    val <!UNUSED_VARIABLE!>x<!> = <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>
}

fun useCompanion() {
    val <!UNUSED_VARIABLE!>d<!> = <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Another<!>
    val <!UNUSED_VARIABLE!>x<!> = Another.<!DEPRECATED_SYMBOL_WITH_MESSAGE!>Companion<!>
    Another.<!DEPRECATED_SYMBOL_WITH_MESSAGE!>Companion<!>.use()
    <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Another<!>.use()
}