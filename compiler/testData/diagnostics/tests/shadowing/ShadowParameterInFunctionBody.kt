// DIAGNOSTICS: +UNUSED_PARAMETER
fun f(<!UNUSED_PARAMETER!>p<!>: Int): Int {
    val <!NAME_SHADOWING!>p<!> = 2
    return p
}
