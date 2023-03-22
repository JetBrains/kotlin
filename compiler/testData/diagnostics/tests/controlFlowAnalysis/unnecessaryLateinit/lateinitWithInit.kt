// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
class Foo {
    <!UNNECESSARY_LATEINIT!>lateinit<!> var bar: String

    init {
        bar = ""
    }
}