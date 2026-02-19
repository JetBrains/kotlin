// LANGUAGE: +HoldsInContracts, +AllowContractsOnSomeOperators
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
import kotlin.contracts.*

inline operator fun Boolean.invoke(block:()-> Unit) {
    contract { this@invoke holdsIn block }
}
