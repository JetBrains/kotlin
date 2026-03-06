// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class AsInt<T: Int>(val value: T) {
    override fun toString(): String {
        return "asInt: ${value.toString()}"
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class AsAny<T: Any>(val value: T) {
    override fun toString(): String {
        return "asAny: ${value.toString()}"
    }
}

fun takeAny(a: Any): String = a.toString()

fun getInt(): Int = 10
fun <T> id(x: T) = x

fun box(): String {
    if (takeAny(AsInt(123)) != "asInt: 123") return "fail"
    if (takeAny(AsAny(321)) != "asAny: 321") return "fail"

    if (takeAny(AsInt(getInt())) != "asInt: 10") return "fail"
    if (takeAny(AsInt(id(20))) != "asInt: 20") return "fail"

    if (takeAny(AsAny(id(30))) != "asAny: 30") return "fail"

    return "OK"
}