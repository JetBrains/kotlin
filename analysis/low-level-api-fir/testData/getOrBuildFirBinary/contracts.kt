// LANGUAGE: +ConditionImpliesReturnsContracts
// LANGUAGE: +HoldsInContracts
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Container

package test

import kotlin.contracts.*

class Container {
    @OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
    fun decode(encoded: String?): String? {
        contract {
            (encoded != null) implies (returnsNotNull())
        }
        if (encoded == null) return null
        return encoded + "a"
    }

    @OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
    inline fun <R> runIf(condition: Boolean, block: () -> R): R? {
        contract { condition holdsIn block }
        return if (condition) {
            block()
        } else null
    }
}
