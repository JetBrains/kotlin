// FIR_IDENTICAL
// JVM_FILE_NAME: ContextReceiversOnFunctionTypeKt
// LANGUAGE: +ContextReceivers
/* KTIJ-28885 */
// KNM_K2_IGNORE

@Target(AnnotationTarget.TYPE)
annotation class MyAnnotation

fun f1(g: context(A, B) Int.(Int) -> Int) {}

fun f2(g: @MyAnnotation context(A, B) Int.(Int) -> Int) {}

fun f3(g: (context(A, B) Int.(Int) -> Int)?) {}

class A {
    val valueA: Int = 10
}

class B {
    val valueB: Int = 11
}
