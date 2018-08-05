// !LANGUAGE: +InlineClasses

inline class Foo<T>(val x: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>T<!>)
inline class FooNullable<T>(val x: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>T?<!>)

inline class Bar(val u: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Unit<!>)
inline class BarNullable(val u: Unit?)

inline class Baz(val u: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Nothing<!>)
inline class BazNullable(val u: Nothing?)
