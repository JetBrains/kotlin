// FIR_IDENTICAL
// LANGUAGE: +DataObjects

class C {
    companion <!WRONG_MODIFIER_TARGET!>data<!> object Object
}
