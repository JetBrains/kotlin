// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// LANGUAGE: +ValueClasses
package pack

@JvmInline
value class Foo<T>(val a: T, val b: T)

@JvmInline
value class MyVa<caret>lueClass(val foo: Foo<Int>)
