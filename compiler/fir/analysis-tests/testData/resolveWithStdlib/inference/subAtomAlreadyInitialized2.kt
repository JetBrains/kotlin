// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71704

@<!OPT_IN_USAGE_ERROR!>OverloadResolutionByLambdaReturnType<!>
public inline fun <T, R> Iterable<T>.foo(transform: (T) -> Iterable<R>): List<R> = listOf()

public inline fun <T, R> Iterable<T>.foo(transform: (T) -> (MutableList<R>.() -> Unit)): List<R> = listOf()

fun testIt(l: List<Int>) {
    l.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!> {
        it -> <!CANNOT_INFER_PARAMETER_TYPE!>{
            <!UNRESOLVED_REFERENCE!>add<!>(it)
        }<!>
    }
}
