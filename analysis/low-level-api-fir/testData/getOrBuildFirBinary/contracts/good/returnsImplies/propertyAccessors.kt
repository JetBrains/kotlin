// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty
// LANGUAGE: +AllowContractsOnPropertyAccessors
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
var Any?.isNotNull: Boolean
    get() {
        contract {
            returns(true) implies (this@isNotNull != null)
        }
        return this != null
    }
    set(value) {
        contract {
            returns() implies (this@isNotNull != null)
        }
        require(this != null)
    }
