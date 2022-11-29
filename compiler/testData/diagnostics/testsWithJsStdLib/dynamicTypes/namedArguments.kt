fun test(d: dynamic) {
    d.foo(name = "name")

    d.foo(1, name = "name")

    d.foo(1, duplicate = "", <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_PASSED_TWICE!>duplicate<!> = "")<!>
}
