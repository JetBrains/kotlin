// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE

var b = true

if (b) {
    val x = 3
}

val y = <!UNRESOLVED_REFERENCE!>x<!>

/* GENERATED_FIR_TAGS: ifExpression, init, integerLiteral, localProperty, propertyDeclaration */
