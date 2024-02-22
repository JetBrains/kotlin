// FIR_IDENTICAL
// JVM_FILE_NAME: ContextReceiversOnTopLevelCallablesKt
// !LANGUAGE: +ContextReceivers
/* Unsupported arguments in test metadata compiler */
// KNM_K2_IGNORE
// KNM_FE10_IGNORE

context(A, B)
private fun Int.function(): Int = valueA + valueB

context(A, B)
private val Int.property: Int get() = valueA + valueB

context(A, B)
private var Int.propertyWithSetter: Int
    get() = valueA + valueB
    set(v) { println(valueA + valueB) }

class A {
    val valueA: Int = 10
}

class B {
    val valueB: Int = 11
}


