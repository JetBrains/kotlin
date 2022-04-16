// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

fun <T> underlying(a: IC<FooHolder>): T = bar(a) {
    it.value.value as T
}

fun <T> extension(a: IC<FooHolder>): T = bar(a) {
    it.extensionValue()
}

fun <T> dispatch(a: IC<FooHolder>): T = bar(a) {
    it.dispatchValue()
}

fun <T> normal(a: IC<FooHolder>): T = bar(a) {
    normalValue(it)
}

fun <T> IC<FooHolder>.extensionValue(): T = value.value as T

fun <T> normalValue(ic: IC<FooHolder>): T = ic.value.value as T

fun <T, R> bar(value: T, f: (T) -> R): R {
    return f(value)
}

interface Foo

class FooHolder(val value: Any): Foo

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC<T: FooHolder>(val value: T): Foo {
    fun <T> dispatchValue(): T = value.value as T
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