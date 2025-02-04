// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74819
// Note: here we don't have OverloadResolutionByLambdaReturnType, so the problem from the issue is not reproducible
// WITH_STDLIB

fun <T, R> Iterable<T>.flatMap(transform: (T) -> Iterable<R>): List<R> = TODO()

fun foo(x: List<String>) {
    buildList {
        add("")
        flatMap { x }
    }
}
