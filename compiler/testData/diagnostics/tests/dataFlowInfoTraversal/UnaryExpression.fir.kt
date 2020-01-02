fun bar(x: Int): Int = x + 1
fun baz(b: Boolean): Boolean = !b

fun foo() {
    val x: Int? = null

    bar(<!INAPPLICABLE_CANDIDATE!>-<!>x)
    if (x != null) bar(-x)
    bar(<!INAPPLICABLE_CANDIDATE!>-<!>x)

    val b: Boolean? = null
    baz(<!INAPPLICABLE_CANDIDATE!>!<!>b)
    if (b != null) baz(!b)
}
