fun ff(<!UNUSED_PARAMETER!>a<!>: String) = 1

fun gg() {
    val a: String? = ""

    if (a != null) {
        ff(a)
    }
}
