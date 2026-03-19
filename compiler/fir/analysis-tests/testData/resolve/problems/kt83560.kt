// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-83560
// WITH_STDLIB

// KT-83560: Consider defining a clearer model explaining the order of entries within TypeVariableFixationReadinessQuality

fun <T> id(x: T): T = x

fun <A, B> combine(a: A, f: (A) -> B): B = f(a)

fun <T : Comparable<T>> max(a: T, b: T): T = if (a > b) a else b

fun test1() {
    val result = combine(1) { x -> x + 1 }
    val r: Int = result
}

fun test2() {
    val result = combine(listOf(1, 2, 3)) { list -> list.size }
    val r: Int = result
}

fun test3() {
    val a = id(42)
    val b = id("hello")
    val c = max(a, 100)
}

fun <T> buildList(vararg elements: T): List<T> = elements.toList()

fun test4() {
    val list = buildList(1, 2, 3)
    val s: List<Int> = list
}

fun <F, S> Pair<F, S>.mapFirst(transform: (F) -> F): Pair<F, S> = Pair(transform(first), second)

fun test5() {
    val pair = Pair(1, "hello")
    val result = pair.mapFirst { it * 2 }
    val r: Pair<Int, String> = result
}

/* GENERATED_FIR_TAGS: additiveExpression, comparisonExpression, funWithExtensionReceiver, functionDeclaration,
functionalType, ifExpression, integerLiteral, lambdaLiteral, localProperty, multiplicativeExpression, nullableType,
outProjection, propertyDeclaration, stringLiteral, typeConstraint, typeParameter, vararg */
