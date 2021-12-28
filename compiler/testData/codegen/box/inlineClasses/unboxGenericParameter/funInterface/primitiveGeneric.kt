// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

fun <T: Int> underlying(a: IC<T>): T = bar(a) { it.value }

fun <T: Int> extension(a: IC<T>): T = bar(a) { it.extensionValue() }

fun <T: Int> dispatch(a: IC<T>): T = bar(a) { it.dispatchValue() }

fun <T: Int> normal(a: IC<T>): T = bar(a) { normalValue(it) }

fun interface FunIFace<T, R> {
    fun call(ic: T): R
}

fun <T, R> bar(value: T, f: FunIFace<T, R>): R {
    return f.call(value)
}

fun <T: Int> IC<T>.extensionValue(): T = value

fun <T: Int> normalValue(ic: IC<T>): T = ic.value

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC<T: Int>(val value: T) {
    fun dispatchValue(): T = value
}

fun box(): String {
    var res = underlying<Int>(IC(40)) + 2
    if (res != 42) "FAIL 1: $res"

    res = extension<Int>(IC(40)) + 3
    if (res != 43) "FAIL 2: $res"

    res = dispatch<Int>(IC(40)) + 4
    if (res != 44) "FAIL 3: $res"

    res = normal<Int>(IC(40)) + 5
    if (res != 45) return "FAIL 4: $res"

    return "OK"
}