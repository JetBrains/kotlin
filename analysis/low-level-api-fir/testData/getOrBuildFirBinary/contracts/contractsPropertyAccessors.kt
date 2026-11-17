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
