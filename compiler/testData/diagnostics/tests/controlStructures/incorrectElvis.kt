// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT
// ISSUE: KT-55932

fun test(x: String?): Int = <!TYPE_MISMATCH!>x?.length ?: "smth"<!>

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, intersectionType, nullableType, safeCall, stringLiteral */
