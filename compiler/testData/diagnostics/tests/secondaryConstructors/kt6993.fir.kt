// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE
class X<T>(val t: T) {
    constructor(t: T, i: Int) : <!INAPPLICABLE_CANDIDATE!>this<!>(i)
}
