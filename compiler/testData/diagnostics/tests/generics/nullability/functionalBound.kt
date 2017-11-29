// !WITH_NEW_INFERENCE
fun <E : String?, T : ((CharSequence) -> Unit)?> foo(x: E, y: T) {
    if (x != null) {
        <!NI;UNSAFE_CALL, OI;UNSAFE_IMPLICIT_INVOKE_CALL!>y<!>(<!NI;TYPE_MISMATCH, DEBUG_INFO_SMARTCAST!>x<!>)
    }

    if (y != null) {
        <!NI;UNSAFE_CALL, OI;DEBUG_INFO_SMARTCAST!>y<!>(<!TYPE_MISMATCH!>x<!>)
    }

    if (x != null && y != null) {
        <!NI;UNSAFE_CALL, OI;DEBUG_INFO_SMARTCAST!>y<!>(<!NI;TYPE_MISMATCH, DEBUG_INFO_SMARTCAST!>x<!>)
    }
}