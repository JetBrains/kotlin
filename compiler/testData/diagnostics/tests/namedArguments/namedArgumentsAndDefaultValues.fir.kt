fun foo(a : Int = 1, b : String = "abc") {
}

fun bar(x : Int = 1, y : Int = 1, z : String) {
}

fun test() {
    foo()
    foo(2)
    <!INAPPLICABLE_CANDIDATE!>foo<!>("")
    foo(b = "")
    foo(1, "")
    foo(a = 2)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(1, "", "")

    bar(z = "")
    <!INAPPLICABLE_CANDIDATE!>bar<!>()
    <!INAPPLICABLE_CANDIDATE!>bar<!>("")
    bar(1, 1, "")
    bar(1, 1, "")
    bar(1, z = "")
    bar(1, z = "", y = 2)
    <!INAPPLICABLE_CANDIDATE!>bar<!>(z = "", 1)
    <!INAPPLICABLE_CANDIDATE!>bar<!>(1, zz = "",
           <!UNRESOLVED_REFERENCE!>zz<!>.<!UNRESOLVED_REFERENCE!>foo<!>
           )
}