// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_EXPRESSION,-UNUSED_VARIABLE

fun <T : CharSequence?> T.bar1() {}
fun CharSequence?.bar2() {}

fun <T : CharSequence> T.bar3() {}

fun <T : String?> foo(x: T) {
    x<!UNSAFE_CALL!>.<!>length
    x?.length

    if (1 == 1) {
        x!!.length
    }


    x.bar1()
    x.bar2()

    x?.bar1()
    x?.bar2()

    x<!UNSAFE_CALL!>.<!>bar3()

    x?.let { it.length }
}
