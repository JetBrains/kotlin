// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-46586

inline fun run(block: () -> Unit) = block()

fun test() {
    var label: String? = null
    run {
        label = "zzz"
    }
    if (label == null) {
        label = "zzz"
    }
    label.isBlank()
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, functionalType, ifExpression, inline,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral */
