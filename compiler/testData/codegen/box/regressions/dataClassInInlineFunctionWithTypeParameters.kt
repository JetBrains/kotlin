// ISSUE: KT-81618
// WITH_STDLIB

data class Wrapper<T>(val x: T)

fun <T, S> Wrapper<T>.mean(space: S): Any = with(space) {
    data class Accumulator(var sum: T, var num: Int)
    Accumulator(x, 1)
}

fun box(): String {
    val string = Wrapper(2).mean(Any()).toString()
    if (string == "") error(string)
    return "OK"
}
