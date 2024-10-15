// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

class Foo() {
    lateinit var bar: String

    constructor(baz: Int) : this() {
        bar = ""
    }
}