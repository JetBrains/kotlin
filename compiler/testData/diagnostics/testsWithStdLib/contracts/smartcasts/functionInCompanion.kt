// FIR_IDENTICAL
// ISSUE: KT-51704

import Foo.Companion.checkSomethingCompanion
import Bar.checkSomethingObject
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class Foo {
    companion object {
        @OptIn(ExperimentalContracts::class)
        fun checkSomethingCompanion(condition: Boolean, message: String) {
            contract {
                returns() implies (condition)
            }
            if (!condition)
                throw Exception(message)
        }
    }
}

object Bar {
    @OptIn(ExperimentalContracts::class)
    fun checkSomethingObject(condition: Boolean, message: String) {
        contract {
            returns() implies (condition)
        }
        if (!condition)
            throw Exception(message)
    }
}

fun usage() {
    Foo.checkSomethingCompanion(1 == 2, "wat")    // ok
    checkSomethingCompanion(1 == 2, "wat")        // compiler crash
    Bar.checkSomethingObject(1 == 2, "wat")    // ok
    checkSomethingObject(1 == 2, "wat")        // compiler crash
}
