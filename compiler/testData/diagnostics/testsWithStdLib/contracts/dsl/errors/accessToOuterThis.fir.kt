// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect +AllowContractsForNonOverridableMembers
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -SENSELESS_COMPARISON

import kotlin.contracts.*

interface A

class Foo {
    inner class Bar {
        fun good() {
            contract {
                returns() implies (this@Bar != null)
            }
        }

        fun badOuter() {
            contract {
                <!ERROR_IN_CONTRACT_DESCRIPTION("'org.jetbrains.kotlin.contracts.description.KtErroneousContractElement@6c410e75' is not a parameter or receiver reference")!>returns() implies (this@Foo != null)<!>
            }
        }

        fun badInner() {
            contract {
                <!ERROR_IN_CONTRACT_DESCRIPTION("'org.jetbrains.kotlin.contracts.description.KtErroneousContractElement@6715868c' is not a parameter or receiver reference")!>returns() implies (this != null)<!>
            }
        }

        fun A?.goodWithReceiver() {
            contract {
                returns() implies (this@goodWithReceiver != null)
            }
        }

        fun A?.badWithReceiver() {
            contract {
                <!ERROR_IN_CONTRACT_DESCRIPTION("'org.jetbrains.kotlin.contracts.description.KtErroneousContractElement@5c086bda' is not a parameter or receiver reference")!>returns() implies (this@Bar != null)<!>
            }
        }
    }
}
