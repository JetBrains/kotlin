// LANGUAGE: +HoldsInContracts
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
// ISSUE: KT-79157
import kotlin.contracts.*

inline fun testDefaultArguments(a: String?, condition: Boolean = a is String, block: () -> Unit) {
    contract { condition holdsIn block }
    block()
}
