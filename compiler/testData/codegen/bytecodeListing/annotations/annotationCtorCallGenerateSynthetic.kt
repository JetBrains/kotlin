// WITH_STDLIB
// IGNORE_BACKEND: JVM
// !LANGUAGE: +InstantiationOfAnnotationClasses

annotation class Foo(val int: Int)

annotation class Bar

fun box() {
    val foo = Foo(42)
}