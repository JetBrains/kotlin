// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
class Flow<out T>

@OverloadResolutionByLambdaReturnType
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
fun <T> Flow<T>.debounce(timeoutMillis: (T) -> Long): Flow<T> = this

@JvmName("debounceDuration")
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun <T> Flow<T>.debounce(timeout: (T) -> String): Flow<T> = this

fun invalidFlow(x: Flow<Int>): Flow<Int> = x.debounce { value -> 0 }

fun foo1(x: Flow<Int>) {}
fun foo2(x: Flow<Any>) {}

fun test(x: Flow<Int>) {
    // We fix `x.debounce { value -> * }` in FULL mode because `Flow` is a contravariant type
    foo1(x.debounce { value -> 1 })
    foo2(x.debounce { value -> 2 })
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, funWithExtensionReceiver, functionDeclaration, functionalType,
lambdaLiteral, nullableType, out, stringLiteral, thisExpression, typeParameter */
