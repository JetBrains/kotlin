fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    bar(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    if (x != 2) {
        if (x == null) return
        2<!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>
    }
    else {
        if (x == null) return
        2<!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!><!SYNTAX!><!>
    }
    bar(x)
}
