fun bar(x: Int): Int = x + 1
fun baz(b: Boolean): Boolean = !b

fun foo() {
    val x: Int? = null

    bar(<!UNSAFE_CALL!>-<!>x)
    if (x != null) bar(-<!DEBUG_INFO_SMARTCAST!>x<!>)
    bar(<!UNSAFE_CALL!>-<!>x)

    val b: Boolean? = null
    baz(<!UNSAFE_CALL!>!<!>b)
    if (b != null) baz(!<!DEBUG_INFO_SMARTCAST!>b<!>)
}
