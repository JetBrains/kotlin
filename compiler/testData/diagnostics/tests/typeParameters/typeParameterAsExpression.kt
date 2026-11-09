// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87577
// RENDER_DIAGNOSTICS_FULL_TEXT
class C<T> {
    inline fun <reified T> f() {
        when (<!NONE_APPLICABLE("In package 'root package' of module '<main>': --> typeParameterAsExpression.kt  |5 | T (of 'fun <T> f')  |reason: Type parameter 'T (of 'fun <T> f')' is not an expression.In package 'root package' of module '<main>': --> typeParameterAsExpression.kt  |4 | T (of 'class C<T>')  |reason: Type parameter 'T (of 'class C<T>')' is not an expression.")!>T<!>) {
        }
        T::class
    }
}

fun <<!REDECLARATION!>T<!>, <!REDECLARATION!>T<!>> f() {
    <!NONE_APPLICABLE!>T<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, typeParameter, whenExpression,
whenWithSubject */
