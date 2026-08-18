// RUN_PIPELINE_TILL: FRONTEND
// DISABLE_NEXT_PHASE_SUGGESTION
// ISSUE: KT-65832
@Suppress(<!ERROR_SUPPRESSION!>"UNRESOLVED" + "_REFERENCE"<!>)
fun foo() {
    undefined()
}

const val DIAG = "UNRESOLVED_REFERENCE"

@Suppress(<!ERROR_SUPPRESSION!>DIAG<!>)
fun bar() {
    undefined()
}

/* GENERATED_FIR_TAGS: const, functionDeclaration, propertyDeclaration, stringLiteral */
