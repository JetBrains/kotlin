// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -DUPLICATE_LABEL_IN_WHEN, -USELESS_IS_CHECK

fun <T> materialize(): T {
    TODO()
}

fun expectedTypeInGuard(x: Any) {
    when(x) {
        is Int if materialize<Boolean>() -> 100
        is String if materialize() -> 200
        is Double if <!CONDITION_TYPE_MISMATCH!>materialize<String>()<!> -> 100
        else -> 0
    }
}

/* GENERATED_FIR_TAGS: andExpression, functionDeclaration, guardCondition, integerLiteral, isExpression, nullableType,
typeParameter, whenExpression, whenWithSubject */
