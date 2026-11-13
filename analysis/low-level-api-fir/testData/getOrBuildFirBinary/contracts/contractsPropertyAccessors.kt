// IGNORE_STANDALONE
// `LLStandaloneFirElementByPsiElementChooser` crashes on building FIR type references with `createEmptySession()`
// LANGUAGE: +AllowContractsOnPropertyAccessors
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty
package test

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
val Boolean.myRequire: Boolean
    get() {
        contract {
            returns(true) implies (this@myRequire)
        }

        return true
    }
