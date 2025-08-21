// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-69191

object None {
    object Type
}

fun <V> typeOf(value: V): Any {
    return when (value) {
        None -> None.Type
        else -> ""
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, nestedClass, nullableType, objectDeclaration,
stringLiteral, typeParameter, whenExpression, whenWithSubject */
