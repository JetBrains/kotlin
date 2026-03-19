// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74820
// WITH_STDLIB

// KT-74820: Investigate using `mergeOtherSystem` at PostponedArgumentsAnalyzer#applyResultsOfAnalyzedLambdaToCandidateSystem

fun <T> runWith(block: () -> T): T = block()

fun <T, R> transform(value: T, block: (T) -> R): R = block(value)

fun <T> combine(first: () -> T, second: () -> T): T {
    return if (first() == second()) first() else second()
}

fun test1() {
    val result = runWith { listOf(1, 2, 3) }
    println(result)
}

fun test2() {
    val result = transform(42) { it * 2 }
    println(result)
}

fun test3() {
    val result = combine({ "hello" }, { "world" })
    println(result)
}

fun test4() {
    // Nested lambdas exercising constraint system merging
    val result = runWith {
        transform("text") { s -> s.length }
    }
    println(result)
}

fun test5() {
    // Lambda with type variable as expected type
    val list = listOf(1, 2, 3)
    val result = list.map { it.toString() }.filter { it.isNotEmpty() }
    println(result)
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, functionalType, ifExpression, integerLiteral,
lambdaLiteral, localProperty, multiplicativeExpression, nullableType, propertyDeclaration, stringLiteral, typeParameter */
