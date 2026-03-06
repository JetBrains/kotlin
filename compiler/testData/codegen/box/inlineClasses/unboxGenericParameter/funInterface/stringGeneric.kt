// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

fun <T: String> underlying(a: IC<T>): T = bar(a) {
    it.value
}

fun <T: String> extension(a: IC<T>): T = bar(a) {
    it.extensionValue()
}

fun <T: String> dispatch(a: IC<T>): T = bar(a) {
    it.dispatchValue()
}

fun <T: String> normal(a: IC<T>): T = bar(a) {
    normalValue(it)
}

fun interface FunIFace<T, R> {
    fun call(ic: T): R
}

fun <T, R> bar(value: T, f: FunIFace<T, R>): R {
    return f.call(value)
}

fun <T: String> IC<T>.extensionValue(): T = value

fun <T: String> normalValue(ic: IC<T>): T = ic.value

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC<T: String>(val value: T) {
    fun dispatchValue(): T = value
}

fun box(): String {
    var res = underlying<String>(IC("O")) + "K"
    if (res != "OK") return "FAIL 1: $res"

    res = extension<String>(IC("O")) + "K"
    if (res != "OK") return "FAIL 2: $res"

    res = dispatch<String>(IC("O")) + "K"
    if (res != "OK") return "FAIL 3: $res"

    res = normal<String>(IC("O")) + "K"
    if (res != "OK") return "FAIL 3: $res"

    return "OK"
}
