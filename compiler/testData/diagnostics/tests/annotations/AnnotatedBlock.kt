// FIR_IDENTICAL
// FIR_DUMP
// ISSUE: KT-52175

annotation class Ann

fun test(x: String?) {
    if (x != null)
        @Ann() { Unit }
}