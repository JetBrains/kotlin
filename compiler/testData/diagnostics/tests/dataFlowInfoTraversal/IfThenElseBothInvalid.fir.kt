fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    bar(x)
    if (x != 2) {
        if (x == null) return
        2<!AMBIGUITY!>+<!><!SYNTAX!><!>
    }
    else {
        if (x == null) return
        2<!AMBIGUITY!>+<!><!SYNTAX!><!>
    }
    bar(x)
}
