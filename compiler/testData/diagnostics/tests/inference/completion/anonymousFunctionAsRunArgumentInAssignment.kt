// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-81383
// WITH_EXTRA_CHECKERS

var obj: Any? = null

fun <T> materialize(): T = obj <!UNCHECKED_CAST!>as T<!>
inline fun <reified T> reifiedMaterialize(): T = obj as T

fun <U> id(u: U) = u
fun <K> select(a: K, b: K) = b

fun test() {
    val x: Int = run(fun() = materialize())
    obj = x
}

fun testReified() {
    val x: Int = run(fun() = reifiedMaterialize())
    obj = x
}

fun testWithIdWrap() {
    val x: Int = id(run(fun() = materialize()))
    obj = x
}

fun testWithSelect() {
    val x: Int = run(select({ materialize() }, fun() = materialize()))
    obj = x
}

/* GENERATED_FIR_TAGS: anonymousFunction, asExpression, assignment, functionDeclaration, inline, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, typeParameter */
