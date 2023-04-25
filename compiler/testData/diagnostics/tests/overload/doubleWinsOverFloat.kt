// FIR_IDENTICAL
// SKIP_TXT
// DIAGNOSTICS: -USELESS_IS_CHECK, -DEBUG_INFO_SMARTCAST
// ISSUE: KT-57194

fun foo(arg: Double) {}
fun foo(arg: Float) {}
fun Double.bar() {}
fun Float.bar() {}

fun test(arg: Any) {
    ::foo

    if (arg is Double) {
        if (arg is Float) {
            foo(arg)
            arg.bar()
        }
    }
}
