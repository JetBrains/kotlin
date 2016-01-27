class A(val x: (String.() -> Unit)?)

fun test(a: A) {
    if (a.x != null) {
        "".<!DEBUG_INFO_SMARTCAST!>(a.x)<!>()
        a.<!UNSAFE_IMPLICIT_INVOKE_CALL!>x<!>("") // todo
        <!DEBUG_INFO_SMARTCAST!>(a.x)<!>("")
    }
    "".<!UNSAFE_IMPLICIT_INVOKE_CALL!>(a.x)<!>()
    a.<!UNSAFE_IMPLICIT_INVOKE_CALL!>x<!>("")
    <!UNSAFE_IMPLICIT_INVOKE_CALL!>(a.x)<!>("")

    with("") {
        a.<!UNSAFE_IMPLICIT_INVOKE_CALL!>x<!>(<!NO_VALUE_FOR_PARAMETER!>)<!>
        <!UNSAFE_IMPLICIT_INVOKE_CALL!>(a.x)<!>()
        if (a.x != null) {
            a.<!UNSAFE_IMPLICIT_INVOKE_CALL!>x<!>(<!NO_VALUE_FOR_PARAMETER!>)<!> // todo
            <!DEBUG_INFO_SMARTCAST!>(a.x)<!>()
        }
    }
}
