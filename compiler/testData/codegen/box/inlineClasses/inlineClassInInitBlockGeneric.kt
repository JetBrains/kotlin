// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

var result = "Fail"

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: String>(val value: T) {
    fun f() = value + "K"
}

class B<T: String>(val a: A<T>) {
    val result: String
    init {
        result = a.f()
    }
}

fun box(): String {
    return B(A("O")).result
}
