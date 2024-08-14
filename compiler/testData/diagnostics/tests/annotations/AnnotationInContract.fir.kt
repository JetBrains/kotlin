import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun foo() {
    contract {
        @<!INFERENCE_ERROR!>foo<!><!SYNTAX!><!>
    }
}
