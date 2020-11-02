// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE
// FIR_IDENTICAL

inline class Foo(val x: Int) {
    init {}

    init {
        val f = 1
    }
}
