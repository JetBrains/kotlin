// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers

annotation class MyAnnotation

context(A, B)
@MyAnnotation
private open class ContextReceiversOnClass {

}

class A {
    val valueA: Int = 10
}

class B {
    val valueB: Int = 11
}