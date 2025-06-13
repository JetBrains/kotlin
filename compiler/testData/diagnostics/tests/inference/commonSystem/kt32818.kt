// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE

fun <T : Any> nullable(): T? = null

fun test() {
    val value = nullable<Int>() ?: nullable()
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, localProperty, nullableType, propertyDeclaration,
typeConstraint, typeParameter */
