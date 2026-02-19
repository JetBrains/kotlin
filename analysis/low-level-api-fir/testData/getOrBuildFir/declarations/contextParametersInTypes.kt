// LANGUAGE: +ContextParameters
package lib

<expr>context(_: T1)
public suspend fun <T1, T2, R> myContext(
    p1: context(A, B) Int.(Int) -> Int,
    p2: @MyAnnotation context(A, B) Int.(Int) -> Int,
    p3: (context(A, B) Int.(Int) -> Int)?,
    p4: suspend context(A, B) Int.(Int) -> Int,
    block: context(T2) () -> R,
): R {
    return null!!
}</expr>

class A {
    val valueA: Int = 10
}

class B {
    val valueB: Int = 11
}

@Target(AnnotationTarget.TYPE)
annotation class MyAnnotation
