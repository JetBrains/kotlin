// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LATEST_LV_DIFFERENCE
class Flow<out T>

@OverloadResolutionByLambdaReturnType
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
fun <T> Flow<T>.debounce(timeoutMillis: (T) -> Long): Flow<T> = this

@JvmName("debounceDuration")
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun <T> Flow<T>.debounce(timeout: (T) -> String): Flow<T> = this

fun invalidFlow(x: Flow<Int>): Flow<Int> = x.<!OVERLOAD_RESOLUTION_AMBIGUITY!>debounce<!> { <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>value<!> -> 0 }

/* GENERATED_FIR_TAGS: classDeclaration, classReference, funWithExtensionReceiver, functionDeclaration, functionalType,
lambdaLiteral, nullableType, out, stringLiteral, thisExpression, typeParameter */
