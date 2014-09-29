class C(val f : () -> Unit)

fun test(e : Any) {
    if (e is C) {
        (<!DEBUG_INFO_SMARTCAST!>e<!>.f)()
    }
}
