// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_EXPRESSION,-UNUSED_VARIABLE

fun <T : CharSequence?> T.bar1() {}
fun CharSequence?.bar2() {}

fun <T : CharSequence> T.bar3() {}
fun CharSequence.bar4() {}

fun <T : CharSequence?> foo(x: T) {

    if (x != null) {
        if (<!SENSELESS_COMPARISON!>x != null<!>) {}

        <!OI;DEBUG_INFO_SMARTCAST!>x<!><!NI;UNSAFE_CALL!>.<!>length
        x<!UNNECESSARY_SAFE_CALL!>?.<!>length

        x.bar1()
        x.bar2()
        x<!NI;UNSAFE_CALL!>.<!><!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>bar3<!>()
        <!OI;DEBUG_INFO_SMARTCAST!>x<!><!NI;UNSAFE_CALL!>.<!>bar4()


        x<!UNNECESSARY_SAFE_CALL!>?.<!>bar1()
    }

    x<!UNSAFE_CALL!>.<!>length

    if (x is String) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        x<!UNNECESSARY_SAFE_CALL!>?.<!>length

        x.bar1()
        x.bar2()
        <!OI;DEBUG_INFO_SMARTCAST!>x<!>.bar3()
    }

    if (x is CharSequence) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        x<!UNNECESSARY_SAFE_CALL!>?.<!>length

        <!NI;DEBUG_INFO_SMARTCAST!>x<!>.bar1()
        x.bar2()
        <!DEBUG_INFO_SMARTCAST!>x<!>.bar3()
    }
}
