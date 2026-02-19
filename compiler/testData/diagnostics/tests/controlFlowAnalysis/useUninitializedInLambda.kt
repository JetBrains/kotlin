// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun bar(f: () -> Unit) = f()

fun foo() {
    var v: Any
    bar { <!UNINITIALIZED_VARIABLE!>v<!>.hashCode() }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, localProperty, propertyDeclaration */
