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
                returns() implies (<!ERROR_IN_CONTRACT_DESCRIPTION("only references to direct <this> are allowed")!>this@Foo<!> != null)
            }
        }

        fun badInner() {
            contract {
                returns() implies (<!ERROR_IN_CONTRACT_DESCRIPTION("only references to parameters are allowed. Did you miss label on <this>?")!>this<!> != null)
            }
        }

        fun A?.goodWithReceiver() {
            contract {
                returns() implies (this@goodWithReceiver != null)
            }
        }

        fun A?.badWithReceiver() {
            contract {
                returns() implies (<!ERROR_IN_CONTRACT_DESCRIPTION("only references to direct <this> are allowed")!>this@Bar<!> != null)
            }
        }
    }
}
