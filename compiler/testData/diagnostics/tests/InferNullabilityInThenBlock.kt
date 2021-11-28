fun ff(a: String) = 1

fun gg() {
    val a: String? = ""

    if (a != null) {
        ff(<!DEBUG_INFO_SMARTCAST!>a<!>)
    }
}
