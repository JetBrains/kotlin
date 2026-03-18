// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-11491
// WITH_STDLIB

// KT-11491: Type Checks and Casts — useless cast and unchecked cast diagnostics

fun testUselessCast() {
    val a = 42 <!USELESS_CAST!>as Int<!> // No cast needed: Int -> Int
    val b = "hello" <!USELESS_CAST!>as String<!> // No cast needed: String -> String
    println(a)
    println(b)
}

fun testUncheckedCast() {
    val list: List<Any> = listOf(1, 2, 3)
    val ints = list <!UNCHECKED_CAST!>as List<Int><!> // UNCHECKED_CAST: List<Any> -> List<Int>
    println(ints)
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, integerLiteral, localProperty, propertyDeclaration,
stringLiteral */
