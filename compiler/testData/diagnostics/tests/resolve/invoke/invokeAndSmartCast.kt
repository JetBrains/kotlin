class A(val x: (String.() -> Unit)?)

fun test(a: A) {
    if (a.x != null) {
        "".<!DEBUG_INFO_SMARTCAST!>(a.x)<!>()
        a.<!UNSAFE_CALL!>x<!>("") // todo
        <!DEBUG_INFO_SMARTCAST!>(a.x)<!>("")
    }
    "".<!UNSAFE_CALL!>(a.x)<!>()
    a.<!UNSAFE_CALL!>x<!>("")
    <!UNSAFE_CALL!>(a.x)<!>("")

    with("") {
        a.<!UNSAFE_CALL!>x<!>(<!NO_VALUE_FOR_PARAMETER!>)<!>
        <!UNSAFE_CALL!>(a.x)<!>()
        if (a.x != null) {
            a.<!UNSAFE_CALL!>x<!>(<!NO_VALUE_FOR_PARAMETER!>)<!> // todo
            <!DEBUG_INFO_SMARTCAST!>(a.x)<!>()
        }
    }
}

fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()