// RUN_PIPELINE_TILL: FRONTEND
val test: Int = if (true) <!TYPE_MISMATCH!>{
    when (2) {
        1 -> 1
        else -> null
    }
}<!>
else {
    2
}

/* GENERATED_FIR_TAGS: equalityExpression, ifExpression, integerLiteral, propertyDeclaration, whenExpression,
whenWithSubject */
