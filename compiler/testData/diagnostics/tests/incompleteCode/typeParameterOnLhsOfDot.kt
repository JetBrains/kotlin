// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
package bar

class S<T> {
    fun foo() {
        <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>T<!>
        <!TYPE_PARAMETER_ON_LHS_OF_DOT!>T<!>.<!UNRESOLVED_REFERENCE!>create<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, typeParameter */
