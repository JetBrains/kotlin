// JVM_FILE_NAME: ContextReceiversOnTopLevelCallablesKt
// !LANGUAGE: +ContextReceivers

class A {
    val value: Int = 10
}

context(A)
private fun Int.function(): Int = value

context(A)
private val Int.property: Int get() = value

context(A)
private var Int.propertyWithSetter: Int
    get() = value
    set(v) { println(value) }

