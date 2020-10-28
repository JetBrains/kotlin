// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE

inline class Foo(val x: Int) {
    init {}

    init {
        val f = 1
    }
}
