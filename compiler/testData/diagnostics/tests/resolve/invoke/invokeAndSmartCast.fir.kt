class A(val x: (String.() -> Unit)?)

fun test(a: A) {
    if (a.x != null) {
        "".<!UNRESOLVED_REFERENCE!>(a.x)()<!>
        a.x("") // todo
        (a.x)("")
    }
    "".<!UNRESOLVED_REFERENCE!>(a.x)()<!>
    a.<!INAPPLICABLE_CANDIDATE!>x<!>("")
    <!INAPPLICABLE_CANDIDATE!>(a.x)("")<!>

    with("") {
        a.<!INAPPLICABLE_CANDIDATE!>x<!>()
        <!INAPPLICABLE_CANDIDATE!>(a.x)()<!>
        if (a.x != null) {
            a.<!INAPPLICABLE_CANDIDATE!>x<!>() // todo
            <!INAPPLICABLE_CANDIDATE!>(a.x)()<!>
        }
    }
}
