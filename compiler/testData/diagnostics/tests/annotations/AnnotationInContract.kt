import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun foo() {
    contract {
        @<!UNRESOLVED_REFERENCE!>foo<!><!SYNTAX!><!>
    }
}
