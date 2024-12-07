// RUN_PIPELINE_TILL: FRONTEND
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun foo() {
    contract {
        <!ANNOTATION_IN_CONTRACT_ERROR!>@<!UNRESOLVED_REFERENCE!>foo<!><!><!SYNTAX!><!>
    }
}
