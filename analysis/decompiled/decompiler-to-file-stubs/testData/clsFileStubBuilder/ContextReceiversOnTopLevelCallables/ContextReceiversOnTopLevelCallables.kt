// FIR_IDENTICAL
// JVM_FILE_NAME: ContextReceiversOnTopLevelCallablesKt
// LANGUAGE: +ContextReceivers

annotation class MyAnnotation

context(A, B)
@MyAnnotation
private fun Int.function(): Int = valueA + valueB

context(A, B)
@MyAnnotation
private val Int.property: Int get() = valueA + valueB

context(A, B)
@MyAnnotation
private var Int.propertyWithSetter: Int
    get() = valueA + valueB
    set(v) { println(valueA + valueB) }

class A {
    val valueA: Int = 10
}

class B {
    val valueB: Int = 11
}


