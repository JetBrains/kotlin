infix fun Int.good(x: Int) {}

<!INAPPLICABLE_INFIX_MODIFIER!>infix fun Int.foo(x: Int, y: Int) {}<!>

<!INAPPLICABLE_INFIX_MODIFIER!>infix fun Int.bar() {}<!>

<!INAPPLICABLE_INFIX_MODIFIER!>infix fun baz(x: Int, y: Int) {}<!>

infix class A

infix typealias B = A

infix val x = 1