// WITH_RUNTIME
// IGNORE_BACKEND: JVM
// !LANGUAGE: +InstantiationOfAnnotationClasses

annotation class Foo(val int: Int)

annotation class Bar

fun box() {
    val foo = Foo(42)
}