import kotlin.contracts.*

class A

class B : A()

contract fun notNull(a: A?, b: A?) = [
    returns() implies (a != null),
    returns() implies (b != null)
]

contract fun argIsStringWhenReturnsTrue(arg: Any?) = [
    returns(true) implies (arg is String)
]

contract fun myContract(a: A?, b: A?, arg: Any?, block: (A, A) -> Int) = [
    notNull(a, b),
    argIsStringWhenReturnsTrue(arg),
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
]

fun foo(arg1: A?, arg2: A?, arg3: Any?, block: (A, A) -> Int): Boolean contract [
    myContract(arg1, arg2, arg3, block)
] {
    require(arg1 != null)
    require(arg2 != null)
    val res = block(arg1, arg2)
    println(res)
    if (arg3 !is String) {
        return false
    }
    return true
}