// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE

inline class Foo(val x: Int) {
    <!INNER_CLASS_INSIDE_INLINE_CLASS!>inner<!> class InnerC
    <!WRONG_MODIFIER_TARGET!>inner<!> object InnerO
    <!WRONG_MODIFIER_TARGET!>inner<!> interface InnerI
}
