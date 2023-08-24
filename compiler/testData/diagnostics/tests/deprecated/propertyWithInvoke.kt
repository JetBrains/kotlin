// FIR_IDENTICAL
@Deprecated("No")
val f: () -> Unit = {}

fun test() {
    <!DEPRECATION!>f<!>()
}
