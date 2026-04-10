// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-34669

// KT-34669: Incorrect USELESS_CAST warning when casting from dynamic type multiple times

fun foo() {
    val dyn: dynamic = 1

    val str: String = dyn as String
    val str1: String = dyn as String // should not warn "No cast needed"
}

fun bar() {
    val dyn: dynamic = 1

    val int1: Int = dyn as Int
    val int2: Int = dyn as Int // should not warn "No cast needed"
    val str: String = dyn as String
    val str1: String = dyn as String // OK in original bug report
}

/* GENERATED_FIR_TAGS: asExpression, flexibleType, functionDeclaration, integerLiteral, intersectionType, localProperty,
propertyDeclaration, smartcast */
