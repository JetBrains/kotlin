fun foo(a : Int = 1, b : String = "abc") {
}

fun bar(x : Int = 1, y : Int = 1, z : String) {
}

fun test() {
    foo()
    foo(2)
    foo(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
    foo(b = "")
    foo(1, "")
    foo(a = 2)
    foo(1, "", <!TOO_MANY_ARGUMENTS!>""<!>)

    bar(z = "")
    bar<!NO_VALUE_FOR_PARAMETER!>()<!>
    bar(<!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>""<!>)<!>
    bar(1, 1, "")
    bar(1, 1, "")
    bar(1, z = "")
    bar(1, z = "", y = 2)
    <!INAPPLICABLE_CANDIDATE!>bar<!>(z = "", 1)
    bar(1, <!NAMED_PARAMETER_NOT_FOUND!>zz<!> = "",
           <!NO_VALUE_FOR_PARAMETER!><!UNRESOLVED_REFERENCE!>zz<!>.foo
           )<!>
}
