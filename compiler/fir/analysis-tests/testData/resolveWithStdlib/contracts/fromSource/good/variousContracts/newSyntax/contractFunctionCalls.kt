import kotlin.contracts.*

contract fun notNull(arg: Any?) = [
    returns() implies (arg != null)
]

contract fun calledOnce(block: () -> Unit) = [
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
]

fun foo(str: String?, block: () -> Unit) contract [
    notNull(str),
    calledOnce(block)
] {
    require(str != null)
    block()
    println(str)
}