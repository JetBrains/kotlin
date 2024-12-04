// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InlineClasses
// DIAGNOSTICS: -UNUSED_VARIABLE, -INLINE_CLASS_DEPRECATED

inline class Foo(val x: Int) {
    <!INNER_CLASS_INSIDE_VALUE_CLASS!>inner<!> class InnerC
    <!WRONG_MODIFIER_TARGET!>inner<!> object InnerO
    <!WRONG_MODIFIER_TARGET!>inner<!> interface InnerI
}
