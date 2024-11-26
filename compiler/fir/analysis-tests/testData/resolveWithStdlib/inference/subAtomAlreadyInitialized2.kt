// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71704

@OverloadResolutionByLambdaReturnType
public inline fun <T, R> Iterable<T>.foo(transform: (T) -> Iterable<R>): List<R> = listOf()

public inline fun <T, R> Iterable<T>.foo(transform: (T) -> (MutableList<R>.() -> Unit)): List<R> = listOf()

fun testIt(l: List<Int>) {
    l.foo {
        it -> {
            add(it)
        }
    }
}
