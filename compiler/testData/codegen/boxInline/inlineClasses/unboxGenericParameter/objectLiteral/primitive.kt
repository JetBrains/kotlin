// !LANGUAGE: +InlineClasses

// FILE: inline.kt

inline class IC(val value: Int) {
    inline fun <T> dispatchInline(): T = value as T
}

inline fun <T> IC.extensionInline(): T = value as T

inline fun <T> normalInline(a: IC): T = a.value as T

// FILE: box.kt
// NO_CHECK_LAMBDA_INLINING

fun <T> extension(a: IC): T = bar(a, object : IFace<IC, T> {
    override fun call(it: IC): T = it.extensionInline()
})

fun <T> dispatch(a: IC): T = bar(a, object : IFace<IC, T> {
    override fun call(it: IC): T = it.dispatchInline()
})

fun <T> normal(a: IC): T = bar(a, object : IFace<IC, T> {
    override fun call(it: IC): T = normalInline(it)
})

interface IFace<T, R> {
    fun call(ic: T): R
}

fun <T, R> bar(value: T, f: IFace<T, R>): R {
    return f.call(value)
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