// COMPILER_ARGUMENTS: -XXLanguage:+AllowContractsForCustomFunctions -XXLanguage:+UseReturnsEffect
@file:Suppress("INVISIBLE_MEMBER")
package test

import kotlin.internal.contracts.*

fun myRequire(x: Boolean) {
    contract {
        returns() implies x
    }
}

fun testContractFromSource(x: Any?, y: Any?) {
    myRequire(x is String)
    myRequire(y is String)

    <caret>x.length

    myRequire(x is Int)
}