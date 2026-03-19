// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-55127
// WITH_STDLIB

// KT-55127: Ensure correctness of fixVariable in NewConstraintSystemImpl
// When fixing a type variable, constraints containing the fixed variable
// are removed from other variables instead of being substituted.
// This can incorrectly relax bounds for further type variables.

fun <A, B : A> coerceIn(value: B, container: MutableList<A>): A {
    container.add(value)
    return value
}

fun <T, R> transform(list: List<T>, f: (T) -> R): List<R> = list.map(f)

// Type variable chain: C <: B <: A
fun <A, B : A, C : B> chain3(a: A, b: B, c: C): Triple<A, B, C> = Triple(a, b, c)

fun test() {
    // Multiple interrelated type variables: A fixed first, B should respect A's fixation
    val container = mutableListOf<Number>()
    val result: Number = coerceIn(42, container) // A=Number, B=Int, B : A

    // Chain with dependent type variable constraints: all Int
    val triple = chain3(42, 42, 42) // A=B=C=Int

    // Chain where types differ but satisfy the bounds
    val triple2 = chain3(42 as Number, 42 as Number, 42) // A=Number, B=Number, C=Int

    // Transform with multiple type variable interactions
    val nums = listOf(1, 2, 3)
    val doubled: List<Int> = transform(nums) { it * 2 }

    // Nested type variables with constraint propagation
    val pairs = listOf(1 to "a", 2 to "b")
    val keys: List<Int> = transform(pairs) { it.first }
    val values: List<String> = transform(pairs) { it.second }
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
multiplicativeExpression, nullableType, propertyDeclaration, stringLiteral, typeConstraint, typeParameter */
