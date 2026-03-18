// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-31660

// KT-31660: Confusing error message when the last statement in lambda is a declaration, not an expression

fun getSomeString(): String = TODO()

val implicitReturn: () -> String = {
    <!RETURN_TYPE_MISMATCH!>val x: String = getSomeString()<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, localProperty, propertyDeclaration */
