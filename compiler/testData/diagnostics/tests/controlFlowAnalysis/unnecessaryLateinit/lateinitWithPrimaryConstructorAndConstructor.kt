// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

class Foo() {
    lateinit var bar: String

    constructor(baz: Int) : this() {
        bar = ""
    }
}