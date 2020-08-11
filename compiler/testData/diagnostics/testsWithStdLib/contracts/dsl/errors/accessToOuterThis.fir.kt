// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect +AllowContractsForNonOverridableMembers
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -SENSELESS_COMPARISON

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
                returns() implies (this@Foo != null)
            }
        }

        fun badInner() {
            contract {
                returns() implies (this != null)
            }
        }

        fun A?.goodWithReceiver() {
            <!WRONG_IMPLIES_CONDITION!>contract {
                returns() implies (this@goodWithReceiver != null)
            }<!>
        }

        fun A?.badWithReceiver() {
            contract {
                returns() implies (this@Bar != null)
            }
        }
    }
}
