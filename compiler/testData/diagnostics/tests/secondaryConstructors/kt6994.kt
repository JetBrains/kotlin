// !DIAGNOSTICS: -UNUSED_PARAMETER
class X<T> {
    constructor(t: T, i: Int): this(<!TYPE_MISMATCH!>i<!>, 1)
}
