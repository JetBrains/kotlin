// NO_CHECK_LAMBDA_INLINING
// LANGUAGE: +InlineClasses
// FILE: inline.kt

inline class IC(val value: String) {
    inline fun <T> dispatchInline(): T = value as T
}

inline fun <T> IC.extensionInline(): T = value as T

inline fun <T> normalInline(a: IC): T = a.value as T

// FILE: box.kt

fun <T> extension(a: IC): T = bar(a) {
    it.extensionInline()
}

fun <T> dispatch(a: IC): T = bar(a) {
    it.dispatchInline()
}

fun <T> normal(a: IC): T = bar(a) {
    normalInline(it)
}

fun interface FunIFace<T, R> {
    fun call(ic: T): R
}

fun <T, R> bar(value: T, f: FunIFace<T, R>): R {
    return f.call(value)
}

fun box(): String {
    var res = extension<String>(IC("O")) + "K"
    if (res != "OK") return "FAIL 2: $res"

    res = dispatch<String>(IC("O")) + "K"
    if (res != "OK") return "FAIL 3: $res"

    res = normal<String>(IC("O")) + "K"
    if (res != "OK") return "FAIL 3: $res"

    return "OK"
}
