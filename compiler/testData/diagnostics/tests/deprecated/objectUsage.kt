@Deprecated("Object")
object Obsolete {
    fun use() {}
}

fun useObject() {
    <!DEPRECATION!>Obsolete<!>.use()
    val <!UNUSED_VARIABLE!>x<!> = <!DEPRECATION!>Obsolete<!>
}
