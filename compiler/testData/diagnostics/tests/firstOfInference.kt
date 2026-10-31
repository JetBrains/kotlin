// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-90176
// WITH_STDLIB
// FIR_DUMP
// LANGUAGE: +ConsiderUseSiteRecursiveBoundsOnCapturedTypes +AvoidStarProjectingSupertypeOfRecursiveCapturedTypes

fun <T : Comparable<T>> firstOf(list: List<*>): T =
    @Suppress("UNCHECKED_CAST") (list[0] as T)

fun main() {
    val x: Any = firstOf(listOf(1, 2))
    println("reached: " + x)
}

/* GENERATED_FIR_TAGS: additiveExpression, asExpression, functionDeclaration, integerLiteral, localProperty,
nullableType, propertyDeclaration, starProjection, stringLiteral, typeConstraint, typeParameter */
