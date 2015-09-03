// !DIAGNOSTICS: -UNUSED_EXPRESSION,-UNUSED_VARIABLE

fun <T : CharSequence?> T.bar1() {}
fun CharSequence?.bar2() {}

fun <T : CharSequence> T.bar3() {}

fun <T, R> T.let(f: (T) -> R): R = f(this)

fun <T : String?> foo(x: T) {
    x<!UNSAFE_CALL!>.<!>length()
    x?.length()

    if (1 == 1) {
        x!!.length()
    }


    x.bar1()
    x.bar2()

    x?.bar1()
    x?.bar2()

    x.<!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>bar3<!>()

    x?.let { it<!UNSAFE_CALL!>.<!>length() }
}
