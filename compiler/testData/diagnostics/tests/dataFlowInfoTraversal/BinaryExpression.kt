// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    bar(1 + (if (x == null) 0 else x))
    bar(<!TYPE_MISMATCH{NI}!>if (x == null) <!DEBUG_INFO_CONSTANT, TYPE_MISMATCH{OI}!>x<!> else x<!>)
    if (x != null) bar(x + x/(x-x*x))
}
