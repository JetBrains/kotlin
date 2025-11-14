// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun whenExtraSpacing(x: Int): String {
    return     when   (  x  ) {   
        1  ->   "one"
        else   ->    "other"
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, stringLiteral, whenExpression,
whenWithSubject */
