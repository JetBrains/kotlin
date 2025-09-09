// LANGUAGE: +ContextReceivers

@Target(AnnotationTarget.TYPE)
annotation class MyAnnotation

fun f1(g: context(A, B) Int.(Int) -> Int) {}

fun f2(g: @MyAnnotation context(A, B) Int.(Int) -> Int) {}

fun f3(g: (context(A, B) Int.(Int) -> Int)?) {}

fun f4(g: suspend context(A, B) Int.(Int) -> Int) {}

class A {
    val valueA: Int = 10
}

class B {
    val valueB: Int = 11
}
