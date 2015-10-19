fun <E : String?, T : ((CharSequence).() -> Unit)?> foo(x: E, y: T) {
    if (x != null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.<!UNSAFE_CALL, INVOKE_EXTENSION_ON_NOT_EXTENSION_FUNCTION!>y<!>()
    }

    if (y != null) {
        x<!UNSAFE_CALL!>.<!><!INVOKE_EXTENSION_ON_NOT_EXTENSION_FUNCTION, DEBUG_INFO_SMARTCAST!>y<!>()
    }

    if (x != null && y != null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.<!INVOKE_EXTENSION_ON_NOT_EXTENSION_FUNCTION, DEBUG_INFO_SMARTCAST!>y<!>()
    }
}