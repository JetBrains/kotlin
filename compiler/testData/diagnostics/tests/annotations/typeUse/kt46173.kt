// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -USELESS_CAST
// ISSUE: KT-46173

@Target(AnnotationTarget.TYPE)
annotation class Ann(val s: String)

fun some(): Int {
    return 1 <!INTEGER_LITERAL_CAST_INSTEAD_OF_TO_CALL!>as @Ann(<!ARGUMENT_TYPE_MISMATCH!>6<!>) Int<!> // should error but doesn't
}

/* GENERATED_FIR_TAGS: annotationDeclaration, asExpression, functionDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration */
