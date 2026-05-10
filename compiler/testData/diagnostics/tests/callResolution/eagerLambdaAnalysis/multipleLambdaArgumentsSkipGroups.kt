// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-85593
// LANGUAGE: +EagerLambdaAnalysis

// Case 1: Non-function expected type (Any parameter) — first group skipped, second discriminates
@JvmName("c1Int")
fun c1(a: Any, b: () -> Int): Int = 1
fun c1(a: () -> Unit, b: () -> String): String = ""

// Case 2: Different parameter count — first group skipped, second discriminates
@JvmName("c2NoParam")
fun c2(a: () -> Unit, b: () -> Int): Int = 1
fun c2(a: (Int) -> Unit, b: () -> String): String = ""

// Case 3: Different input types — first group skipped, second discriminates
@JvmName("c3Int")
fun c3(a: (Int) -> Unit, b: () -> Int): Int = 1
fun c3(a: (String) -> Unit, b: () -> String): String = ""

fun main() {
    // Case 1: first group has null expectedType (Any) -> skip, second group resolves
    val x1 = c1({}, { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x1<!>

    // Case 2: first group has different param count (0 vs 1) -> skip, second group resolves
    val x2 = c2({}, { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x2<!>

    // Case 3: first group has different input types (Int vs String) -> skip, second group resolves
    val x3 = c3({ _ -> }, { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x3<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral */
