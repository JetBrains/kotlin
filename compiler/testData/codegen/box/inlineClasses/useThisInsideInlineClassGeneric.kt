// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt<T: Int>(val a: T) {
    fun test() {
        takeNullable(this)
        takeAnyInside(this)

        takeAnyInside(this.a)
    }

    fun takeAnyInside(a: Any) {}
}

fun <T: Int> takeNullable(a: UInt<T>?) {}

fun box(): String {
    val a = UInt(0)
    a.test()

    return "OK"
}