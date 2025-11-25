// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81777, KT-76150
// LANGUAGE: +CollectionLiterals

// Even with enabled collection literals and implemented fallback we might want "unresolved" collection literal in some cases,
// e.g. when standard library is missing.

fun testWithLambdas() {
    val lam: Array<() -> Unit> = [{}]
    val withParam: Array<(Int) -> Unit> = [{ it -> }]
    val withParamOfSpecifiedType: Array<(Int) -> Unit> = [{ it: Any -> }]
    val withReturn: Array<() -> Int> = [{ 42 }]
    val withReturnAndParam: Array<(Int) -> Int> = [{ x -> x }]

    [{}]
    [{ it -> }]
    [{ it: Any -> }]
    [{ 42 }]
    [{ x -> x }]
}

fun testWithAnons() {
    val anon: Array<() -> Unit> = [fun() {}]
    val withParam: Array<(Int) -> Unit> = [fun(x: Int) {}]
    val withReturn: Array<() -> Int> = [fun() = 42]
    val withReturnAndParam: Array<(Int) -> Int> = [fun(x: Int) = x]

    [fun() {}]
    [fun(x: Int) {}]
    [fun() = 42]
    [fun(x: Int) = x]
}

fun skip() {}
fun <T> id(it: T) = it
fun const42() = 42
fun <T> consume(it: T) {}

fun testWithCallables() {
    val callable: Array<() -> Unit> = [::skip]
    val withParam: Array<(Int) -> Unit> = [::consume]
    val withReturn: Array<() -> Int> = [::const42]
    val withReturnAndParam: Array<(Int) -> Int> = [::id]
}
