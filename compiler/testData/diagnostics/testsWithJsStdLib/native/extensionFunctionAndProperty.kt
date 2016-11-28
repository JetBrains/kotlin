class A

<!WRONG_MODIFIER_TARGET!>external fun A.foo(): Unit = noImpl<!>

<!WRONG_MODIFIER_TARGET!>external var A.bar: String
    get() = noImpl
    set(value) = noImpl<!>

<!WRONG_MODIFIER_TARGET!>external val A.baz: String
    get() = noImpl<!>