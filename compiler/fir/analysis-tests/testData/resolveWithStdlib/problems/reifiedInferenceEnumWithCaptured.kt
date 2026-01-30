// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// DUMP_INFERENCE_LOGS: FIXATION

enum class MyEnum { A, B }

fun test() = when {
    true -> <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>enumValueOf<!>("A")
    else -> MyEnum.B
}

/* GENERATED_FIR_TAGS: capturedType, enumDeclaration, enumEntry, functionDeclaration, starProjection, stringLiteral,
whenExpression */
