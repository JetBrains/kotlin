// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87577
// RENDER_DIAGNOSTICS_FULL_TEXT
class C<T> {
    inline fun <reified T> f() {
        when (<!NONE_APPLICABLE("T:  Type parameter 'T' is not an expression.T:  Type parameter 'T' is not an expression.")!>T<!>) {
        }
        T::class
    }
}

fun <<!REDECLARATION!>T<!>, <!REDECLARATION!>T<!>> f() {
    <!NONE_APPLICABLE!>T<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, typeParameter, whenExpression,
whenWithSubject */
