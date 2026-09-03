// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LateinitVals

lateinit val x: Any

fun test() {
    if (x is String) {
        <!SMARTCAST_IMPOSSIBLE!>x<!>.length
    }
}
