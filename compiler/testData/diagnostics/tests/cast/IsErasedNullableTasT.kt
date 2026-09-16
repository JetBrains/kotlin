// RUN_PIPELINE_TILL: CODEGEN
fun <T: Any> testing(a: T?) = a is T

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression, nullableType, typeConstraint, typeParameter */
