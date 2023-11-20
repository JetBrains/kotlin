// SKIP_KT_DUMP
// !LANGUAGE: +SuspendConversion

// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57755: Fix computing a mangled name for the `main` function

fun main() {
    val foo: String.(suspend () -> Unit) -> Unit = {}
    val f: () -> Unit = {}
    "".foo(f)
}
