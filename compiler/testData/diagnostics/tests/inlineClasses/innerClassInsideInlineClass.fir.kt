// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE

inline class Foo(val x: Int) {
    inner class InnerC
    inner object InnerO
    inner interface InnerI
}
