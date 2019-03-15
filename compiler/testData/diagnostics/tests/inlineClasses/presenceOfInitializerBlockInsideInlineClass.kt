// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE

inline class Foo(val x: Int) {
    <!INLINE_CLASS_WITH_INITIALIZER!>init<!> {}

    <!INLINE_CLASS_WITH_INITIALIZER!>init<!> {
        val f = 1
    }
}
