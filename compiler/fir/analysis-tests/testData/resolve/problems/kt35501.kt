// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-35501

// KT-35501: Too large highlighting range for "Type mismatch" with qualified return inside a lambda that returns specified type

fun <R> block(block: () -> R): R = block()

val a: String =
    block {
        <!RETURN_TYPE_MISMATCH!>return@block<!>
    }

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, nullableType, propertyDeclaration,
typeParameter */
