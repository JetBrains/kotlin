// ISSUE: KT-56744
// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect +AllowContractsForNonOverridableMembers
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

open class Foo {
    fun myRun(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        block()
    }

    fun myRequire(x: Boolean) {
        contract { returns() implies (x) }
    }

    inline fun <reified T> assertIs(value: Any) {
        contract { returns() implies (value is T) }
    }
}

class Bar : Foo() {
    fun test_1(x: Any) {
        myRequire(x is String)
        x.length
    }

    fun test_2(x: Any) {
        assertIs<String>(x)
        x.length
    }

    fun test_3(): Int {
        val x: Int
        myRun {
            x = 1
        }
        return x + 1
    }
}

fun test_1(foo: Foo, x: Any) {
    foo.myRequire(x is String)
    x.length
}

fun test_2(foo: Foo, x: Any) {
    foo.assertIs<String>(x)
    x.length
}

fun test_3(foo: Foo): Int {
    val x: Int
    foo.myRun {
        x = 1
    }
    return x + 1
}
