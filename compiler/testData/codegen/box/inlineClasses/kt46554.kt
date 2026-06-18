// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS

var result = "Fail"

OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val value: String) {
    constructor() : this("OK")

    init {
        result = value
    }
}

fun box(): String {
    A()
    return result
}
