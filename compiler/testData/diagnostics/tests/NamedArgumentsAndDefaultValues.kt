fun foo(<!UNUSED_PARAMETER!>a<!> : Int = 1, <!UNUSED_PARAMETER!>b<!> : String = "abc") {
}

fun bar(<!UNUSED_PARAMETER!>x<!> : Int = 1, <!UNUSED_PARAMETER!>y<!> : Int = 1, <!UNUSED_PARAMETER!>z<!> : String) {
}

fun test() {
    foo()
    foo(2)
    foo(<!TYPE_MISMATCH!>""<!>)
    foo(b = "")
    foo(1, "")
    foo(a = 2)
    foo(1, "", <!TOO_MANY_ARGUMENTS!>""<!>)

    bar(z = "")
    bar<!NO_VALUE_FOR_PARAMETER!>()<!>
    bar<!NO_VALUE_FOR_PARAMETER!>("")<!>
    bar(1, 1, "")
    bar(1, 1, "")
    bar(1, <!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>z<!> = "")
    bar<!NO_VALUE_FOR_PARAMETER!>(1, <!NAMED_PARAMETER_NOT_FOUND, MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>zz<!> = "", <!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!><!UNRESOLVED_REFERENCE!>zz<!>.foo<!>)<!>
}
