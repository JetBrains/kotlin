// FIR_IDENTICAL
// JVM_FILE_NAME: ContextReceiversOnFunctionTypeKt
// !LANGUAGE: +ContextReceivers
/* Unsupported arguments in test metadata compiler */
// KNM_K2_IGNORE
// KNM_FE10_IGNORE

fun f(g: context(A, B) Int.(Int) -> Int) {}

class A {
    val valueA: Int = 10
}

class B {
    val valueB: Int = 11
}
