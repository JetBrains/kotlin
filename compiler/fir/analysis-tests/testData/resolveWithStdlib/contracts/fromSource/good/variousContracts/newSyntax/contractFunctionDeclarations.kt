import kotlin.contracts.*

contract fun test1(arg: Any?) = [
    returns() implies (arg != null)
]

contract fun test2(block: () -> Unit) = [
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
]

contract fun test3(arg: Any?, block: () -> Unit) = [
    test1(arg),
    test2(block)
]