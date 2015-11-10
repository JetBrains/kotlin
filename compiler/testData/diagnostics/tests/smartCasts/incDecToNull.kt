class IncDec {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc(): Unit {}
}

fun foo(): IncDec {
    var x = IncDec()
    x = <!UNUSED_CHANGED_VALUE!>x<!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!><!>
    x<!INC_DEC_SHOULD_NOT_RETURN_UNIT!>++<!>
    return x
}
