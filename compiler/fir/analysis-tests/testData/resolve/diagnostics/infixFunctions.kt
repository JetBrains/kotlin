infix fun Int.good(x: Int) {}

<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun Int.foo(x: Int, y: Int) {}

<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun Int.bar() {}

<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun baz(x: Int, y: Int) {}

<!WRONG_MODIFIER_TARGET!>infix<!> class A

<!WRONG_MODIFIER_TARGET!>infix<!> typealias B = A

<!WRONG_MODIFIER_TARGET!>infix<!> val x = 1

class C {
    infix fun good(x: Int) {}
    infix fun Int.goodAsWell(x: Int) {}

    <!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun Int.foo(x: Int, y: Int) {}

    <!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun Int.bar() {}

    <!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun baz(x: Int, y: Int) {}
}
