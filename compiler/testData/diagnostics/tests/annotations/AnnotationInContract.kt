// RUN_PIPELINE_TILL: FRONTEND
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun foo() {
    contract {
        @<!UNRESOLVED_REFERENCE!>foo<!><!SYNTAX!><!>
    }
}

/* GENERATED_FIR_TAGS: classReference, contracts, functionDeclaration, lambdaLiteral */
