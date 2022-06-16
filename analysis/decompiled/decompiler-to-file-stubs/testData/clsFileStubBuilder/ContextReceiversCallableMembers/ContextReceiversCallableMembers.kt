// !LANGUAGE: +ContextReceivers

private open class ContextReceiversCallableMembers {
    context(A)
    private fun Int.function(): Int = value

    context(A)
    private val Int.property: Int get() = value

    context(A)
    private var Int.propertyWithSetter: Int
        get() = value
        set(v) { println(value) }
}

class A {
    val value: Int = 10
}

