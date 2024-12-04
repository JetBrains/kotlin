// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses

@JvmInline
value class Empty<T><!VALUE_CLASS_EMPTY_CONSTRUCTOR!>()<!>


@JvmInline
value class Foo<T>(val x: T, val y: T)

@JvmInline
value class FooNullable<T>(val x: T?, val y: T?)


@JvmInline
value class FooGenericArray<T>(val x: Array<T>, val y: Array<T>)

@JvmInline
value class FooGenericArray2<T>(val x: Array<Array<T>>, val y: Array<Array<T>>)


@JvmInline
value class FooStarProjectedArray(val x: Array<*>, val y: Array<*>)

@JvmInline
value class FooStarProjectedArray2(val x: Array<Array<*>>, val y: Array<Array<*>>)


@JvmInline
value class Bar(val u: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Unit<!>, val y: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Unit<!>)

@JvmInline
value class BarNullable(val u: Unit?, val y: Unit?)


@JvmInline
value class Baz(val u: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Nothing<!>, val y: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Nothing<!>)

@JvmInline
value class Baz1(val u: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Nothing<!>, val y: Int)

@JvmInline
value class Baz2(val u: Int, val y: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Nothing<!>)

@JvmInline
value class BazNullable(val u: Nothing?, val y: Nothing?)
