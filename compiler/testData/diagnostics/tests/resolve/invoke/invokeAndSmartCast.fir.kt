class A(val x: (String.() -> Unit)?)

fun test(a: A) {
    if (a.x != null) {
        "".(<!UNRESOLVED_REFERENCE!>a.x<!>)()
        a.x("") // todo
        (a.x)("")
    }
    "".(<!UNRESOLVED_REFERENCE!>a.x<!>)()
    a.<!UNSAFE_IMPLICIT_INVOKE_CALL!>x<!>("")
    <!UNSAFE_IMPLICIT_INVOKE_CALL!>(a.x)<!>("")

    with("") {
        a.x(<!NO_VALUE_FOR_PARAMETER!>)<!>
        (a.x)(<!NO_VALUE_FOR_PARAMETER!>)<!>
        if (a.x != null) {
            a.x(<!NO_VALUE_FOR_PARAMETER!>)<!> // todo
            (a.x)()
        }
    }
}
