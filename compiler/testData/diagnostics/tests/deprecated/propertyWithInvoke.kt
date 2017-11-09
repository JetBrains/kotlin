@Deprecated("No")
val f: () -> Unit = {}

fun test() {
    <!DEPRECATION!>f<!>()
}
