// !DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_PARAMETER

class VariadicClass<<!WRONG_MODIFIER_TARGET!>vararg<!> Ts>

<!WRONG_MODIFIER_TARGET!>inline<!> fun <vararg Ts> foo() = "FOO"

<!WRONG_MODIFIER_TARGET!>infix<!> fun <vararg Ts> Unit.foo(other: Unit) {}

open class Base {
    <!WRONG_MODIFIER_TARGET!>open<!> fun <vararg Ts> foo() {}
}

class Derived : Base() {
    <!WRONG_MODIFIER_TARGET!>override<!> fun <vararg Ts> foo() {}
}

class Baz {
    <!WRONG_MODIFIER_TARGET!>operator<!> fun <vararg Ts> get(index: Int) {}
}

fun <<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> Ts1, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> Ts2> bar() {}