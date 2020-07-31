// !DIAGNOSTICS: -UNUSED_PARAMETER
class X<T> {
    constructor(t: T, i: Int): <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(i, 1)
}
