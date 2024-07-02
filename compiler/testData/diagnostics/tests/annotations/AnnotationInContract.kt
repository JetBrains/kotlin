// FIR_IDENTICAL
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun foo(): Int? {
    contract {
        returns(@<!UNRESOLVED_REFERENCE!>foo<!><!SYNTAX!><!>)
    }

    return null
}
