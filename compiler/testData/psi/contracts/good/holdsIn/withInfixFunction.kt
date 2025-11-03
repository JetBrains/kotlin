// LANGUAGE: +HoldsInContracts
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
import kotlin.contracts.*

inline infix fun Boolean.trueIn(block:()-> Unit) {
    contract { this@trueIn holdsIn block }
}
