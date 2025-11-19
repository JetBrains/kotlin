// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71704

@<!OPT_IN_USAGE_ERROR!>OverloadResolutionByLambdaReturnType<!>
public inline fun <T, R> Iterable<T>.foo(transform: (T) -> Iterable<R>) {}

public inline fun <T, R> Iterable<T>.foo(transform: (T) -> (MutableList<R>.() -> Unit)) {}

fun testIt(l: List<Int>) {
    l.foo {
        it -> {
            add(it)
        }
    }
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, inline, lambdaLiteral,
nullableType, typeParameter, typeWithExtension */
