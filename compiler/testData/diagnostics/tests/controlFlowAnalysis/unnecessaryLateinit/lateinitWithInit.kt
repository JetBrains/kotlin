// FIR_IDENTICAL
class Foo {
    <!UNNECESSARY_LATEINIT!>lateinit<!> var bar: String

    init {
        bar = ""
    }
}