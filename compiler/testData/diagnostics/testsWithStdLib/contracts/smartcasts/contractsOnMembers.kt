// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect +AllowContractsForNonOverridableMembers
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

class Foo {
    fun myRun(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        block()
    }

    fun require(x: Boolean) {
        contract { returns() implies (x) }
    }
}

fun test_1(foo: Foo, x: Any) {
    foo.require(x is String)
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
}

fun test_2(foo: Foo): Int {
    val x: Int
    foo.myRun {
        x = 1
    }
    return x + 1
}
