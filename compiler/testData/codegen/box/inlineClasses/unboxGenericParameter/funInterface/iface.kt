// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: SAM_CONVERSIONS
// !LANGUAGE: +InlineClasses

fun <T> underlying(a: IC): T = bar(a) {
    (it.value as FooHolder).value as T
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

fun <T> IC.extensionValue(): T = (value as FooHolder).value as T

fun <T> normalValue(ic: IC): T = (ic.value as FooHolder).value as T

fun interface FunIFace<T, R> {
    fun call(ic: T): R
}

fun <T, R> bar(value: T, f: FunIFace<T, R>): R {
    return f.call(value)
}

interface Foo

class FooHolder(val value: Any): Foo

inline class IC(val value: Foo): Foo {
    fun <T> dispatchValue(): T = (value as FooHolder).value as T
}

fun box(): String {
    var res = underlying<Int>(IC(FooHolder(40))) + 2
    if (res != 42) return "FAIL 1: $res"

    res = extension<Int>(IC(FooHolder(40))) + 3
    if (res != 43) return "FAIL 2: $res"

    res = dispatch<Int>(IC(FooHolder(40))) + 4
    if (res != 44) return "FAIL 3: $res"

    res = normal<Int>(IC(FooHolder(40))) + 5
    if (res != 45) return "FAIL 4: $res"

    return "OK"
}