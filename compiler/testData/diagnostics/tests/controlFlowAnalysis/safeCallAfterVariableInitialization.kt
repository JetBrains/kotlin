// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-41198, KT-59860

fun test() {
    val b: Int
    run { b = 1 }<!UNNECESSARY_SAFE_CALL!>?.<!>let {}
    <!UNINITIALIZED_VARIABLE!>b<!>.inc()
}
