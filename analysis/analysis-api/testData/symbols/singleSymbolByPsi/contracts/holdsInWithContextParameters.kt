// WITH_STDLIB
// LANGUAGE: +HoldsInContracts, +ContextParameters

@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
import kotlin.contracts.*

context(a: Boolean)
inline fun cond<caret>itionInContext(block: () -> Int) {
    contract { a holdsIn block }
    block()
}
