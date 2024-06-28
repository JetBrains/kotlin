// FIR_IDENTICAL
// JVM_FILE_NAME: ContractsKt

/* Contract information is stored for .proto-based stubs, but not for source stubs */
// KNM_K2_IGNORE

package test

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun myRequire(x: Boolean) {
    contract {
        returns(true) implies (x)
    }
}