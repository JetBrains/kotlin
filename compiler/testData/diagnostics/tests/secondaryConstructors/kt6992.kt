class X<T>(val t: T) {
    constructor(t: String): <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(t)
}
