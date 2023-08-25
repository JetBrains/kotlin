// FIR_IDENTICAL
// JVM_FILE_NAME: ContextReceiversOnFunctionTypeKt
// !LANGUAGE: +ContextReceivers
// KNM_K2_IGNORE
fun f(g: context(A, B) Int.(Int) -> Int) {}

class A {
    val valueA: Int = 10
}

class B {
    val valueB: Int = 11
}
