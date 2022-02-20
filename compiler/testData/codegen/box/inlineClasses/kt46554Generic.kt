// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

var result = "Fail"

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: String>(val value: T) {
    constructor() : this("OK" as T)

    init {
        result = value
    }
}

fun box(): String {
    A<String>()
    return result
}
