// !LANGUAGE: +SuspendConversion

fun myApply(f: suspend () -> Unit) {}

fun test(f: () -> Unit) {
    myApply(f)
}
