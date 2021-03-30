// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE

inline class Foo(val x: Int) {
    <!INNER_CLASS_INSIDE_INLINE_CLASS!>inner<!> class InnerC
    <!INNER_CLASS_INSIDE_INLINE_CLASS!>inner<!> object InnerO
    <!INNER_CLASS_INSIDE_INLINE_CLASS!>inner<!> interface InnerI
}
