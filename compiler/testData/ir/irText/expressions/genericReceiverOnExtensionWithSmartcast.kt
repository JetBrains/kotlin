// FIR_IDENTICAL
fun <F : CharSequence> F.bar() {}

inline fun <reified T : CharSequence> test_1(x: Any) {
    if (x is T) {
        x.bar()
    }
}

fun test_2(x: Any?) {
    if (x is CharSequence) {
        x.bar()
    }
}
