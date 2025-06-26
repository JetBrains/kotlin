// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
package test

import kotlin.contracts.*

class MyClass {
    @OptIn(ExperimentalContracts::class)
    constructor(x: Boolean) {
        contract {
            returns(true) implies (x)
        }
    }
}
