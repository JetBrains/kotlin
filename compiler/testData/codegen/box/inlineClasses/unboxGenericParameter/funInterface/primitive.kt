// WITH_STDLIB

fun <T1> underlying(a: IC): T1 = bar(a) { it.value as T1 }

fun <T2> extension(a: IC): T2 = bar(a) { it.extensionValue() }

fun <T3> dispatch(a: IC): T3 = bar(a) { it.dispatchValue() }

fun <T4> normal(a: IC): T4 = bar(a) { normalValue(it) }

fun interface FunIFace<T0, R> {
    fun call(ic: T0): R
}

fun <T5, R> bar(value: T5, f: FunIFace<T5, R>): R {
    return f.call(value)
}

fun <T6> IC.extensionValue(): T6 = value as T6

fun <T7> normalValue(ic: IC): T7 = ic.value as T7

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class IC(val value: Int) {
    fun <T8> dispatchValue(): T8 = value as T8
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