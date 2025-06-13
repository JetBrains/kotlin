// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// DIAGNOSTICS: -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS

fun <E : Enum<E>> createMap(enumClass: Class<E>) {}

fun reproduce() {
    val enumClass: Class<Enum<*>> = "any" as Class<Enum<*>>
    createMap(<!ARGUMENT_TYPE_MISMATCH!>enumClass<!>)
}

/* GENERATED_FIR_TAGS: asExpression, capturedType, functionDeclaration, localProperty, propertyDeclaration,
starProjection, stringLiteral, typeConstraint, typeParameter */
