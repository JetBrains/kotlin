fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    bar(if (x == null) 0 else <!DEBUG_INFO_SMARTCAST!>x<!>)

    if (x == null) {
        bar(<!DEBUG_INFO_CONSTANT, TYPE_MISMATCH!>x<!>)
        return
    } else {
        bar(<!DEBUG_INFO_SMARTCAST!>x<!>)
    }
    bar(<!DEBUG_INFO_SMARTCAST!>x<!>)

    val y: Int? = null
    if (y is Int) {
        bar(<!DEBUG_INFO_SMARTCAST!>y<!>)
    } else {
        bar(<!TYPE_MISMATCH!>y<!>)
        return
    }
    bar(<!DEBUG_INFO_SMARTCAST!>y<!>)

    val z: Int? = null
    if (z != null) bar(<!DEBUG_INFO_SMARTCAST!>z<!>)
    bar(<!TYPE_MISMATCH!>z<!>)
    bar(z!!)
    if (<!SENSELESS_COMPARISON!>z != null<!>) bar(z<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
}
