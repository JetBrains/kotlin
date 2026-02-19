// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81948
// DIAGNOSTICS: -UNCHECKED_CAST
// LANGUAGE: +DiscriminateNothingAsNullabilityConstraintInInference
// FIR_DUMP
// DUMP_INFERENCE_LOGS: FIXATION

fun <R> myRun(x: () -> R): R = x()

fun <T> materialize(): T? = "" as T?

// R should be inferred to String?, not to Nothing?
val x: String? = myRun { materialize() }!!

/* GENERATED_FIR_TAGS: asExpression, checkNotNullCall, functionDeclaration, functionalType, lambdaLiteral, nullableType,
propertyDeclaration, stringLiteral, typeParameter */
