@file:OptIn(kotlin.contracts.ExperimentalContracts::class)
import kotlin.contracts.*

fun cas<caret>e_2(): Boolean {
    contract { returns(null) implies case_3() }
    return true
}

fun case_3(): Boolean {
    contract { returns(null) implies case_2() }
    return true
}
