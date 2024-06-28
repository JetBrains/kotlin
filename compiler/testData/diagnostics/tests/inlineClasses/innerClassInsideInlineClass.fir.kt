// LANGUAGE: +InlineClasses, -JvmInlineValueClasses
// DIAGNOSTICS: -UNUSED_VARIABLE

inline class Foo(val x: Int) {
    <!INNER_CLASS_INSIDE_VALUE_CLASS!>inner<!> class InnerC
    <!INNER_CLASS_INSIDE_VALUE_CLASS, WRONG_MODIFIER_TARGET!>inner<!> object InnerO
    <!INNER_CLASS_INSIDE_VALUE_CLASS, WRONG_MODIFIER_TARGET!>inner<!> interface InnerI
}
