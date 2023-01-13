// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
@Deprecated("Object")
object Obsolete {
    fun use() {}
}

fun useObject() {
    <!DEPRECATION!>Obsolete<!>.use()
    val x = <!DEPRECATION!>Obsolete<!>
}
