interface A {
    var foo: String
}

class B(override <!VAR_OVERRIDDEN_BY_VAL!>val<!> foo: String) : A
