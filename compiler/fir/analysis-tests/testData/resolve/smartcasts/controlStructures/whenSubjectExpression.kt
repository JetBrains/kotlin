// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
// ISSUE: KT-49860

fun whenWithSubjectExpression(x: Any) {
    when (x) {
        !is Double -> -1
        0.0 -> 0 // `subj` in `subj == 0.0` must have type 'double'
        else -> x.toInt()
    }
}

fun whenWithSubjectVariable(x: Any) {
    when (val y = x) {
        !is Double -> -1
        0.0 -> 0 // `subj` in `subj == 0.0` must have type 'double'
        else -> y.toInt()
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, isExpression, localProperty,
propertyDeclaration, smartcast, whenExpression, whenWithSubject */
