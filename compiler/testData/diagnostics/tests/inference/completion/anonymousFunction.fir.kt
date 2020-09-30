// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun take(fn: () -> List<String>) {}
fun <L> inferFromLambda(fn: () -> L): L = TODO()
fun <L> inferFromLambda2(fn: (Int) -> L): L = TODO()

fun <T> materialize(): T = TODO()
fun <I> id(arg: I) = arg

fun testFunctions() {
    take { materialize() }
    take(fun() = materialize())
    take(fun(): List<String> = materialize())
    take(fun(): List<String> {
        return materialize()
    })
}

fun testNestedCalls() {
    id<String>(inferFromLambda { materialize() })
    <!INAPPLICABLE_CANDIDATE!>id<!><String>(inferFromLambda(fun() = materialize()))
    <!INAPPLICABLE_CANDIDATE!>id<!><String>(inferFromLambda2(fun() = materialize()))
}
