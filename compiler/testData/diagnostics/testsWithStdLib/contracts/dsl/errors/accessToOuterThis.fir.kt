// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect +AllowContractsForNonOverridableMembers
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -SENSELESS_COMPARISON

import kotlin.contracts.*

interface A

class Foo {
    inner class Bar {
        fun good() {
            contract {
                <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (this<!UNRESOLVED_LABEL!>@Bar<!> != null)<!>
            }
        }

        fun badOuter() {
            contract {
                <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (this<!UNRESOLVED_LABEL!>@Foo<!> != null)<!>
            }
        }

        fun badInner() {
            contract {
                <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (this != null)<!>
            }
        }

        fun A?.goodWithReceiver() {
            <!WRONG_IMPLIES_CONDITION!>contract {
                returns() implies (this@goodWithReceiver != null)
            }<!>
        }

        fun A?.badWithReceiver() {
            contract {
                <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (this<!UNRESOLVED_LABEL!>@Bar<!> != null)<!>
            }
        }
    }
}
