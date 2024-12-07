// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// LANGUAGE: +ValueClasses
package pack

@JvmInline
value class MyValu<caret>eClass(val foo: Foo<Int>?)

@JvmInline
value class Foo<T>(val a: T, val b: T)