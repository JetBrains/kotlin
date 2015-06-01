fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    if (x != null) {
        when (<!DEBUG_INFO_SMARTCAST!>x<!>) {
            0 -> bar(<!DEBUG_INFO_SMARTCAST!>x<!>)
            else -> {}
        }
    }

    when (x) {
        0 -> { if (<!SENSELESS_COMPARISON!>x == null<!>) return }
        else -> { if (x == null) return }
    }
    bar(<!DEBUG_INFO_SMARTCAST!>x<!>)
}
