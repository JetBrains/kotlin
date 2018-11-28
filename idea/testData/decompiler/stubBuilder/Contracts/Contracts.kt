// JVM_FILE_NAME: ContractsKt

package test

import kotlin.contracts.*

@UseExperimental(ExperimentalContracts::class)
fun myRequire(x: Boolean) {
    contract {
        returns(true) implies (x)
    }
}