// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun smartFail(x: Any?) {
    when (x) {
        is String -> println(x.length)
        is Int -> println(x.<!UNRESOLVED_REFERENCE!>length<!>)
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression, nullableType, smartcast, whenExpression, whenWithSubject */
