// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    constructor(
        <!VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER!>val<!> x: Int, y: Int,
        <!VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER!>var<!> z: Int,
        <!WRONG_MODIFIER_TARGET!>public<!> a: Int)
}
