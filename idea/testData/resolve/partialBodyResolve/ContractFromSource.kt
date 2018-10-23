// COMPILER_ARGUMENTS: -XXLanguage:+AllowContractsForCustomFunctions -XXLanguage:+UseReturnsEffect
package test

import kotlin.contracts.*

@UseExperimental(ExperimentalContracts::class)
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