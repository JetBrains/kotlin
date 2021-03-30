fun foo(first: Int, second: Double, third: Boolean, fourth: String) {}

fun test() {
    foo(1, 2.0, true, "")
    foo(1, 2.0, true, fourth = "!")
    foo(1, 2.0, fourth = "???", third = false)
    foo(1, second = 3.14, third = false, fourth = "!?")
    foo(third = false, second = 2.71, fourth = "?!", first = 0)

    <!INAPPLICABLE_CANDIDATE!>foo<!>()
    foo(<!ARGUMENT_TYPE_MISMATCH!>0.0<!>, <!ARGUMENT_TYPE_MISMATCH!>false<!>, <!ARGUMENT_TYPE_MISMATCH!>0<!>, "")
    foo(1, 2.0, third = true, "")
    <!INAPPLICABLE_CANDIDATE!>foo<!>(second = 0.0, first = 0, fourth = "")
    foo(first = <!ARGUMENT_TYPE_MISMATCH!>0.0<!>, second = <!ARGUMENT_TYPE_MISMATCH!>0<!>, third = <!ARGUMENT_TYPE_MISMATCH!>""<!>, fourth = <!ARGUMENT_TYPE_MISMATCH!>false<!>)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(first = 0, second = 0.0, third = false, fourth = "", first = 1)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(0, 0.0, false, foth = "")
}
