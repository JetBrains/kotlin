// OPT_IN: kotlin.contracts.ExperimentalContracts
// ISSUE: KT-69964

import kotlin.contracts.*

abstract class Base {
    @OptIn(ExperimentalContracts::class)
    protected inline fun inPlace(block: () -> Unit) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        block()
    }
}

class Derived : Base() {
    fun test(): Int {
        inPlace {
            return 0
        }
    }
}

abstract class SubstitutionBase<T> {
    @OptIn(ExperimentalContracts::class)
    protected inline fun inPlaceSubstitution(block: () -> Unit) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        block()
    }
}

class SubstitutionDerived : SubstitutionBase<Int>() {
    fun testSubstitution(): Int {
        inPlaceSubstitution {
            return 0
        }
    }
}
