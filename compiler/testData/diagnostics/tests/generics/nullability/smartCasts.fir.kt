// !DIAGNOSTICS: -UNUSED_EXPRESSION,-UNUSED_VARIABLE

fun <T : CharSequence?> T.bar1() {}
fun CharSequence?.bar2() {}

fun <T : CharSequence> T.bar3() {}
fun CharSequence.bar4() {}

fun <T : CharSequence?> foo(x: T) {

    if (x != null) {
        if (x != null) {}

        x.length
        x<!UNNECESSARY_SAFE_CALL!>?.<!>length

        x.bar1()
        x.bar2()
        x.bar3()
        x.bar4()


        x<!UNNECESSARY_SAFE_CALL!>?.<!>bar1()
    }

    x<!UNSAFE_CALL!>.<!>length

    if (x is String) {
        x.length
        x<!UNNECESSARY_SAFE_CALL!>?.<!>length

        x.bar1()
        x.bar2()
        x.bar3()
    }

    if (x is CharSequence) {
        x.length
        x<!UNNECESSARY_SAFE_CALL!>?.<!>length

        x.bar1()
        x.bar2()
        x.bar3()
    }
}
