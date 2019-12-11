// !LANGUAGE: +InlineClasses

inline class Foo<T>(val x: T)
inline class FooNullable<T>(val x: T?)

inline class FooGenericArray<T>(val x: Array<T>)
inline class FooGenericArray2<T>(val x: Array<Array<T>>)

inline class FooStarProjectedArray(val x: Array<*>)
inline class FooStarProjectedArray2(val x: Array<Array<*>>)

inline class Bar(val u: Unit)
inline class BarNullable(val u: Unit?)

inline class Baz(val u: Nothing)
inline class BazNullable(val u: Nothing?)
