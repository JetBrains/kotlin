// !DIAGNOSTICS: -UNUSED_PARAMETER
class X<T>(val t: T) {
    constructor(t: T, i: Int) : this(<!TYPE_MISMATCH!>i<!>)
}