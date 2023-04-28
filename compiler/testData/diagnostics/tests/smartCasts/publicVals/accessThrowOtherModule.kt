// FIR_IDENTICAL
// ISSUE: KT-57425
// WITH_STDLIB

data class Data(val x: Int?)

fun test(pair: Pair<String?, Data>) {
    if (pair.second.x != null) {
        <!SMARTCAST_IMPOSSIBLE!>pair.second.x<!>.inc() // should be an error
    }
    if (pair.first != null) {
        <!SMARTCAST_IMPOSSIBLE!>pair.first<!>.length // should be an error
    }
}
