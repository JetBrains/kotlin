// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

fun <T: Any> underlying(a: IC<T>): T = bar(a) {
    it.value
}

fun <T: Any> extension(a: IC<T>): T = bar(a) {
    it.extensionValue()
}

fun <T: Any> dispatch(a: IC<T>): T = bar(a) {
    it.dispatchValue()
}

fun <T: Any> normal(a: IC<T>): T = bar(a) {
    normalValue(it)
}

fun <T: Any> IC<T>.extensionValue(): T = value

fun <T: Any> normalValue(ic: IC<T>): T = ic.value

fun <T, R> bar(value: T, f: (T) -> R): R {
    return f(value)
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC<T: Any>(val value: T) {
    fun dispatchValue(): T = value
}

fun box(): String {
    var res = underlying<Int>(IC(40)) + 2
    if (res != 42) return "FAIL 1: $res"

    res = extension<Int>(IC(40)) + 3
    if (res != 43) return "FAIL 2: $res"

    res = dispatch<Int>(IC(40)) + 4
    if (res != 44) return "FAIL 3: $res"

    res = normal<Int>(IC(40)) + 5
    if (res != 45) return "FAIL 4: $res"

    return "OK"
}