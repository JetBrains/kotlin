class A(val x: (String.() -> Unit)?)

fun test(a: A) {
    if (a.x != null) {
        "".(a.x)()
        a.x("") // todo
        (a.x)("")
    }
    "".<!UNSAFE_IMPLICIT_INVOKE_CALL!>(a.x)<!>()
    a.<!UNSAFE_IMPLICIT_INVOKE_CALL!>x<!>("")
    <!UNSAFE_IMPLICIT_INVOKE_CALL!>(a.x)<!>("")

    with("") {
        a.x<!NO_VALUE_FOR_PARAMETER!>()<!>
        <!UNSAFE_IMPLICIT_INVOKE_CALL!>(a.x)<!>()
        if (a.x != null) {
            a.x<!NO_VALUE_FOR_PARAMETER!>()<!> // todo
            (a.x)()
        }
    }
}
