// ISSUE: KT-41198, KT-59860

fun test() {
    val b: Int
    run { b = 1 }<!UNEXPECTED_SAFE_CALL!>?.<!>let {}
    b.inc()
}
