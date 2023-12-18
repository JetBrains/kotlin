// SKIP_KT_DUMP
// !LANGUAGE: +SuspendConversion

fun runMe() {
    val foo: String.(suspend () -> Unit) -> Unit = {}
    val f: () -> Unit = {}
    "".foo(f)
}
