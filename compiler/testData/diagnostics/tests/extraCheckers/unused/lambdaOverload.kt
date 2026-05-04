// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-76632
// DUMP_CFG

fun test1(): Boolean {
    var loadData = false
    listOf("").invokeInline {
        loadData = true
        emptyList<String>()
    }
    return loadData
}

inline fun <T, R> Iterable<T>.invokeInline(transform: (T) -> Iterable<R>): List<R> {
    return flatMap(transform)
}

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName("invokeInlineSequence")
inline fun <T, R> Iterable<T>.invokeInline(transform: (T) -> Sequence<R>): List<R> {
    return flatMap(transform)
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral, whenExpression */
