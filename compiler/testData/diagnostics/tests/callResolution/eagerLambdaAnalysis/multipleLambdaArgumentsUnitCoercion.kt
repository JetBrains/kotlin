// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-85593
// LANGUAGE: +EagerLambdaAnalysis

// Case 1: One overload needs unit-coercion for the trailing lambda, the other doesn't
@JvmName("b1Value")
fun b1(a: () -> Unit, b: () -> String): String = ""
fun b1(a: () -> Unit, b: () -> Unit) {}

// Case 2: Different arguments cause unit-coercion for different overloads -> ambiguity
@JvmName("b2First")
fun b2(a: () -> String, b: () -> Unit): String = ""
@JvmName("b2Second")
fun b2(a: () -> Unit, b: () -> String): String = ""

fun main() {
    // Case 1: b1 with { "" } as trailing — the () -> String overload is preferred (no unit-coercion)
    val c1 = b1({ println("a") }) { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>c1<!>

    // Case 2: both overloads have unit-coercion for one lambda each -> ambiguity remains
    val c2 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>b2<!>({ "" }) { "" }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, localProperty, propertyDeclaration,
stringLiteral */
