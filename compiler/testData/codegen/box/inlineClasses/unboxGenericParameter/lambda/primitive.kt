// !LANGUAGE: +InlineClasses

fun <T> underlying(a: IC): T = bar(a) {
    it.value as T
}

fun <T> extension(a: IC): T = bar(a) {
    it.extensionValue()
}

fun <T> dispatch(a: IC): T = bar(a) {
    it.dispatchValue()
}

fun <T> normal(a: IC): T = bar(a) {
    normalValue(it)
}

fun <T, R> bar(value: T, f: (T) -> R): R {
    return f(value)
}

fun <T> IC.extensionValue(): T = value as T

fun <T> normalValue(ic: IC): T = ic.value as T

inline class IC(val value: Int) {
    fun <T> dispatchValue(): T = value as T
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