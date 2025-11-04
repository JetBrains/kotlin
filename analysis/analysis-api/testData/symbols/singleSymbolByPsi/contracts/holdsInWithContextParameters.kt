// WITH_STDLIB
// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// LANGUAGE: +HoldsInContracts, +ContextParameters

@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
import kotlin.contracts.*

context(a: Boolean)
inline fun cond<caret>itionInContext(block: () -> Int) {
    contract { a holdsIn block }
    block()
}
