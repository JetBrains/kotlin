// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    if (x != null && bar(x) == 0) bar(bar(x))
    bar(<!TYPE_MISMATCH!>x<!>)
    if (x == null || bar(x) == 0) bar(bar(<!TYPE_MISMATCH!>x<!>))
    bar(<!TYPE_MISMATCH!>x<!>)
    if (x is Int && bar(x)*bar(x) == bar(x)) bar(x)
    bar(<!TYPE_MISMATCH!>x<!>)
}
