import java.util.HashMap

fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null
    val a = Array<Int>(3, {0})

    for (p in a) {
        bar(<!TYPE_MISMATCH!>x<!>)
        if (x == null) continue
        bar(<!DEBUG_INFO_AUTOCAST!>x<!>)
        for (q in a) {
            bar(<!DEBUG_INFO_AUTOCAST!>x<!>)
            if (<!SENSELESS_COMPARISON!>x == null<!>) bar(<!DEBUG_INFO_AUTOCAST!>x<!>)
        }
    }

    for (p in a) {
        bar(<!TYPE_MISMATCH!>x<!>)
        if (x == null) break
        bar(<!DEBUG_INFO_AUTOCAST!>x<!>)
    }
}
