// !DIAGNOSTICS: -UNUSED_PARAMETER
class X<T> {
    constructor(t: T, i: Int): <!INAPPLICABLE_CANDIDATE!>this<!>(i, 1)
}
