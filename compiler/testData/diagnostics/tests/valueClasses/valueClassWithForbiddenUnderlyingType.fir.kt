// !LANGUAGE: +InlineClasses

package kotlin

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
value class Bar(val u: Unit)
@JvmInline
value class BarNullable(val u: Unit?)

@JvmInline
value class Baz(val u: Nothing)
@JvmInline
value class BazNullable(val u: Nothing?)
