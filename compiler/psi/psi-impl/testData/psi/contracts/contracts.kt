package test

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun myRequire(x: Boolean) {
    contract {
        returns(true) implies (x)
    }
}
