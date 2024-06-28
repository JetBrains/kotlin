// FIR_IDENTICAL
// ALLOW_KOTLIN_PACKAGE
// SKIP_JAVAC
// SKIP_TXT
// LANGUAGE: +InlineClasses

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Foo<T>(val x: T)
@JvmInline
value class FooNullable<T>(val x: T?)

@JvmInline
value class FooGenericArray<T>(val x: Array<T>)
@JvmInline
value class FooGenericArray2<T>(val x: Array<Array<T>>)

@JvmInline
value class FooStarProjectedArray(val x: Array<*>)
@JvmInline
value class FooStarProjectedArray2(val x: Array<Array<*>>)

@JvmInline
value class Bar(val u: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Unit<!>)
@JvmInline
value class BarNullable(val u: Unit?)

@JvmInline
value class Baz(val u: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Nothing<!>)
@JvmInline
value class BazNullable(val u: Nothing?)
