// ISSUE: KT-81618
// WITH_STDLIB

data class Wrapper<T>(val x: T)

fun <T, R> regularWith(value: T, body: T.() -> R): R = body(value)

fun <T, S> Wrapper<T>.mean(space: S): Any = regularWith(space) {
    data class Accumulator(var sum: T, var num: Int)
    Accumulator(x, 1)
}

fun <T, S> Wrapper<T>.mean1(space: S): Any = regularWith(space, fun S.(): Any {
    data class Accumulator(var sum: T, var num: Int)
    return Accumulator(x, 1)
})

fun box(): String {
    val string = Wrapper(2).mean(Any()).toString()
    if (string == "") error(string)
    val string1 = Wrapper(2).mean1(Any()).toString()
    if (string1 == "") error(string1)
    return "OK"
}
