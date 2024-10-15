// RUN_PIPELINE_TILL: FRONTEND
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun foo() {
    contract {
        @<!UNRESOLVED_REFERENCE!>foo<!><!SYNTAX!><!>
    }
}
