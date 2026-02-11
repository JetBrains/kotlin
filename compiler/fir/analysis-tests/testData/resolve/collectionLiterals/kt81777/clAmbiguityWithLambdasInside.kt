// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +CollectionLiterals, +UnnamedLocalVariables

fun <T> select(vararg x: T): T = x[0]

fun ambiguities() {
    val _: Set<*> = select(<!CANNOT_INFER_PARAMETER_TYPE!>mutableSetOf<!>(), <!AMBIGUOUS_COLLECTION_LITERAL!>[{}]<!>)
    val _: Set<*> = select(<!CANNOT_INFER_PARAMETER_TYPE!>mutableSetOf<!>(), <!AMBIGUOUS_COLLECTION_LITERAL!>[{ x: Int -> }]<!>)
    val _: Set<*> = select(<!CANNOT_INFER_PARAMETER_TYPE!>mutableSetOf<!>(), <!AMBIGUOUS_COLLECTION_LITERAL!>[{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>x<!> -> }]<!>)
}

fun noAmbiguities() {
    val _: Set<*> = select(setOf(), [{}])
    val _: Set<*> = select(setOf(), [{ x: Int -> }])
    val _: Set<*> = select(<!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>(), <!CANNOT_INFER_PARAMETER_TYPE!>[{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>x<!> -> }]<!>)
    val _: Set<(Int) -> Unit> = select(setOf(), [{ x -> }])
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, integerLiteral, lambdaLiteral, localProperty, nullableType,
outProjection, propertyDeclaration, starProjection, typeParameter, unnamedLocalVariable, vararg */
