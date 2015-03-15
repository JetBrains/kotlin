// !DIAGNOSTICS: -UNUSED_PARAMETER
class X<T> {
    constructor(t: T, i: Int): <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(<!TYPE_MISMATCH!>i<!>, 1) { // no error
    }
}
