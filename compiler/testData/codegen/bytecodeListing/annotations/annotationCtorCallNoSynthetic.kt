// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

annotation class Foo(val bar: Bar)

annotation class Bar

@Foo(Bar())
fun box() {
}