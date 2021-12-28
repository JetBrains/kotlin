// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: String>(val a: T)

fun <T: String> isEqualNA(x: A<T>?, y: A<T>) = x == y
fun <T: String> isEqualAN(x: A<T>, y: A<T>?) = x == y

fun box(): String {
    if (isEqualNA(null, A(""))) return "Fail 1"
    if (isEqualAN(A(""), null)) return "Fail 2"
    if (!isEqualNA(A(""), A(""))) return "Fail 3"
    if (!isEqualAN(A(""), A(""))) return "Fail 4"
    return "OK"
}
