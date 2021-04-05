// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE
class X<T>(val t: T) {
    constructor(t: T, i: Int) : this(<!ARGUMENT_TYPE_MISMATCH!>i<!>)
}
