// !LANGUAGE: +InlineClasses

package kotlin

annotation class JvmInline

@JvmInline
value class Foo<T>(val x: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>T<!>)
@JvmInline
value class FooNullable<T>(val x: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>T?<!>)

@JvmInline
value class FooGenericArray<T>(val x: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Array<T><!>)
@JvmInline
value class FooGenericArray2<T>(val x: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Array<Array<T>><!>)

@JvmInline
value class FooStarProjectedArray(val x: Array<*>)
@JvmInline
value class FooStarProjectedArray2(val x: Array<Array<*>>)

@JvmInline
value class Bar(val u: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Unit<!>)
@JvmInline
value class BarNullable(val u: Unit?)

@JvmInline
value class Baz(val u: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Nothing<!>)
@JvmInline
value class BazNullable(val u: Nothing?)
