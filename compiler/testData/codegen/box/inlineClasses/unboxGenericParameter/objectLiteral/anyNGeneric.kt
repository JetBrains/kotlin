// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

fun <T> underlying(a: IC<T>): T = bar(a, object : IFace<IC<T>, T> {
    override fun call(ic: IC<T>): T = ic.value
})

fun <T> extension(a: IC<T>): T = bar(a, object : IFace<IC<T>, T> {
    override fun call(ic: IC<T>): T = ic.extensionValue()
})

fun <T> dispatch(a: IC<T>): T = bar(a, object : IFace<IC<T>, T> {
    override fun call(ic: IC<T>): T = ic.dispatchValue()
})

fun <T> normal(a: IC<T>): T = bar(a, object : IFace<IC<T>, T> {
    override fun call(ic: IC<T>): T = normalValue(ic)
})

interface IFace<T, R> {
    fun call(ic: T): R
}

fun <T, R> bar(value: T, f: IFace<T, R>): R {
    return f.call(value)
}

fun <T> IC<T>.extensionValue(): T = value

fun <T> normalValue(ic: IC<T>): T = ic.value

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC<T>(val value: T) {
    fun dispatchValue(): T = value
}

fun box(): String {
    var res = underlying<Int>(IC(40)) + 2
    if (res != 42) "FAIL 1 $res"

    res = extension<Int>(IC(40)) + 3
    if (res != 43) return "FAIL 2: $res"

    res = dispatch<Int>(IC(40)) + 4
    if (res != 44) return "FAIL 3: $res"

    res = normal<Int>(IC(40)) + 5
    if (res != 45) return "FAIL 4: $res"

    return "OK"
}