// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-52262

fun test_1(name: String?) {
    when (name) {
        null -> return
    }
    name.length
}

fun test_2(name: String?) {
    when (val s = name) {
        null -> return
    }
    name.length
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, localProperty, nullableType, propertyDeclaration,
smartcast, whenExpression, whenWithSubject */
