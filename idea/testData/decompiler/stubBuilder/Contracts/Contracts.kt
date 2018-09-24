// JVM_FILE_NAME: ContractsKt

@file:Suppress("INVISIBLE_MEMBER")
package test

import kotlin.internal.contracts.*

fun myRequire(x: Boolean) {
    contract {
        returns(true) implies (x)
    }
}