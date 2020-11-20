// !DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.contracts.*

fun foo(arg: Any?, num: Int?, block: () -> Unit) contract <!UNSUPPORTED!>[
    returns() implies (arg is String),
    returns() implies (num != null),
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
]<!> {
    require(arg is String)
    require(num != null)
    block()
}

fun bar(arg: Any?, block: () -> Int): Boolean contract <!UNSUPPORTED!>[
    returns(true) implies (arg != null),
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
]<!> {
    val num = block()
    if (arg != null) {
        return true
    }
    return false
}