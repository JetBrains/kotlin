// LANGUAGE: +AllowContractsOnPropertyAccessors
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
