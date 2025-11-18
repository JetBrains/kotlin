// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71704

@<!OPT_IN_USAGE_ERROR!>OverloadResolutionByLambdaReturnType<!>
public inline fun <T, R> Iterable<T>.foo(transform: (T) -> Iterable<R>) {}

public inline fun <T, R> Iterable<T>.foo(transform: (T) -> (MutableList<R>.() -> Unit)) {}

fun testIt(l: List<Int>) {
    l.<!CANNOT_INFER_PARAMETER_TYPE!>foo<!> {
        it -> <!CANNOT_INFER_IT_PARAMETER_TYPE!>{
            <!UNRESOLVED_REFERENCE!>add<!>(it)
        }<!>
    }
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, inline, lambdaLiteral,
nullableType, typeParameter, typeWithExtension */
