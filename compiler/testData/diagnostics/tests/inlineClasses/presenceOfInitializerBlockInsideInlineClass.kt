// FIR_IDENTICAL
// LANGUAGE: +InlineClasses
// DIAGNOSTICS: -UNUSED_VARIABLE, -INLINE_CLASS_DEPRECATED

inline class Foo(val x: Int) {
    init {}

    init {
        val f = 1
    }
}
