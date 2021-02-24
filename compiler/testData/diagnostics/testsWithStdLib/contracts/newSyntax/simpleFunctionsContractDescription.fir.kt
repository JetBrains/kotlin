import kotlin.contracts.*

fun printStr(str: String?) contract [
    returns() implies (str != null)
] {
    require(str != null)
    println(str)
}

fun callExactlyOnce(block: () -> Int) contract [
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
] {
    val num = block()
    println(num)
}

fun calculateNumber(block: () -> Int): Int contract [
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
] {
    val num = block()
    return num
}