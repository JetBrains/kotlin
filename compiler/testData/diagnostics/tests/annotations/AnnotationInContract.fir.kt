import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun foo() {
    contract {
        @<!ANNOTATION_IN_CONTRACT_ERROR!>foo<!><!SYNTAX!><!>
    }
}
