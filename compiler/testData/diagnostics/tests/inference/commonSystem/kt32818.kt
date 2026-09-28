// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -UNUSED_VARIABLE

fun <T : Any> nullable(): T? = null

fun test() {
    val value = nullable<Int>() ?: nullable()
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, localProperty, nullableType, propertyDeclaration,
typeConstraint, typeParameter */
