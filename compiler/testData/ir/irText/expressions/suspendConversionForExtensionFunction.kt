// SKIP_KT_DUMP
// !LANGUAGE: +SuspendConversion

fun main() {
    val foo: String.(suspend () -> Unit) -> Unit = {}
    val f: () -> Unit = {}
    "".foo(f)
}
