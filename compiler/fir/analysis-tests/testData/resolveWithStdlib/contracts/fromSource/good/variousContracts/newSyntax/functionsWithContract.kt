import kotlin.contracts.*

fun test1(arg: Any?) contract [
    returns() implies (arg != null)
] {
    require(arg != null)
}

fun test2(s: String?, block: () -> Unit) contract [
    returns() implies (s != null),
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
] {
    require(s != null)
    block()
}

fun test3(arg: Any?): Boolean contract [
    returns(true) implies (arg != null)
] {
    require(arg != null)
    return true
}