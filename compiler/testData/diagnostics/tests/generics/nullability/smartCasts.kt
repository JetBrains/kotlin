// !DIAGNOSTICS: -UNUSED_EXPRESSION,-UNUSED_VARIABLE

fun <T : CharSequence?> T.bar1() {}
fun CharSequence?.bar2() {}

fun <T : CharSequence> T.bar3() {}
fun CharSequence.bar4() {}

fun <T : CharSequence?> foo(x: T) {

    if (x != null) {
        if (<!SENSELESS_COMPARISON!>x != null<!>) {}

        <!DEBUG_INFO_SMARTCAST!>x<!>.length()
        x<!UNNECESSARY_SAFE_CALL!>?.<!>length()

        x.bar1()
        x.bar2()
        x.<!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>bar3<!>()
        <!DEBUG_INFO_SMARTCAST!>x<!>.bar4()


        x<!UNNECESSARY_SAFE_CALL!>?.<!>bar1()
    }

    x<!UNSAFE_CALL!>.<!>length()

    if (x is String) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length()
        <!DEBUG_INFO_SMARTCAST!>x<!><!UNNECESSARY_SAFE_CALL!>?.<!>length()

        <!DEBUG_INFO_SMARTCAST!>x<!>.bar1()
        x.bar2()
        <!DEBUG_INFO_SMARTCAST!>x<!>.bar3()
    }

    if (x is CharSequence) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length()
        x<!UNNECESSARY_SAFE_CALL!>?.<!>length()

        <!DEBUG_INFO_SMARTCAST!>x<!>.bar1()
        x.bar2()
        <!DEBUG_INFO_SMARTCAST!>x<!>.bar3()
    }
}
