fun foo(f: () -> Unit) {}
fun bar(x: Int, f: () -> Unit) {}
fun baz(f: () -> Unit, other: Boolean = true) {}


fun test() {
    // OK
    foo {}
    foo() {}
    foo({})

    // Bad
    foo(<!ARGUMENT_TYPE_MISMATCH!>1<!>) <!TOO_MANY_ARGUMENTS!>{}<!>
    foo(f = {}) <!TOO_MANY_ARGUMENTS!>{}<!>

    // OK
    bar(1) {}
    bar(x = 1) {}
    bar(1, {})
    bar(x = 1, f = {})

    // Bad
    <!NO_VALUE_FOR_PARAMETER!>bar<!> {}
    bar(<!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>{}<!>)<!>

    // OK
    baz(other = false, f = {})
    baz({}, false)

    // Bad
    <!NO_VALUE_FOR_PARAMETER!>baz<!> <!ARGUMENT_TYPE_MISMATCH!>{}<!>
    baz<!NO_VALUE_FOR_PARAMETER!>()<!> <!ARGUMENT_TYPE_MISMATCH!>{}<!>
    baz(<!NO_VALUE_FOR_PARAMETER!>other = false)<!> <!TOO_MANY_ARGUMENTS!>{}<!>
}
