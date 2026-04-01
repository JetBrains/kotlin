// LANGUAGE: +AllowContractsOnPropertyAccessors
import kotlin.contracts.*

interface A {
    fun foo()
}

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
