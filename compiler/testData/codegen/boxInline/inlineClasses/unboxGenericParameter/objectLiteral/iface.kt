// NO_CHECK_LAMBDA_INLINING
// !LANGUAGE: +InlineClasses

// FILE: inline.kt

interface Foo

class FooHolder(val value: Any): Foo

inline class IC(val value: Foo): Foo {
    inline fun <T> dispatchInline(): T = (value as FooHolder).value as T
}

inline fun <T> IC.extensionInline(): T = (value as FooHolder).value as T

inline fun <T> normalInline(a: IC): T = (a.value as FooHolder).value as T

// FILE: box.kt

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
    var res = extension<Int>(IC(FooHolder(40))) + 3
    if (res != 43) return "FAIL 2: $res"

    res = dispatch<Int>(IC(FooHolder(40))) + 4
    if (res != 44) return "FAIL 3: $res"

    res = normal<Int>(IC(FooHolder(40))) + 5
    if (res != 45) return "FAIL 4: $res"

    return "OK"
}
