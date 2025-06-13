// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-67212
fun withClues(vararg clues: () -> Any?) {
    arrayOf({ "" }, *clues)
}

interface Box<T> where T : CharSequence {
    val value: T
}

fun withClues(box: Box<out <!UPPER_BOUND_VIOLATED!>() -> Any?<!>>) {
    arrayOf({ "" }, box.value)
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, interfaceDeclaration, lambdaLiteral, nullableType,
outProjection, propertyDeclaration, stringLiteral, typeConstraint, typeParameter, vararg */
