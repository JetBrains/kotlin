// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers
/* Unsupported arguments in test metadata compiler */
// KNM_K2_IGNORE
// KNM_FE10_IGNORE

context(A, B)
private open class ContextReceiversOnClass {

}

class A {
    val valueA: Int = 10
}

class B {
    val valueB: Int = 11
}