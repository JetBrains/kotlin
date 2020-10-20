// !LANGUAGE: +InlineClasses

// FILE: inline.kt

inline class IC(val value: Any?) {
    inline fun <T> dispatchInline(): T = value as T
}

inline fun <T> IC.extensionInline(): T = value as T

inline fun <T> normalInline(a: IC): T = a.value as T

// FILE: box.kt
// NO_CHECK_LAMBDA_INLINING

fun <T> extension(a: IC): T = bar(a) {
    it.extensionInline()
}

fun <T> dispatch(a: IC): T = bar(a) {
    it.dispatchInline()
}

fun <T> normal(a: IC): T = bar(a) {
    normalInline(it)
}

fun <T, R> bar(value: T, f: (T) -> R): R {
    return f(value)
}

fun box(): String {
    var res = extension<Int>(IC(40)) + 3
    if (res != 43) return "FAIL 2: $res"

    res = dispatch<Int>(IC(40)) + 4
    if (res != 44) return "FAIL 3: $res"

    res = normal<Int>(IC(40)) + 5
    if (res != 45) return "FAIL 4: $res"

    return "OK"
}