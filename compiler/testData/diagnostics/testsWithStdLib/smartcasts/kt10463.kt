// RUN_PIPELINE_TILL: BACKEND
val test: Int = listOf<Any>().map {
    when (it) {
        is Int -> <!DEBUG_INFO_SMARTCAST!>it<!>
        else -> throw AssertionError()
    }
}.sum()

/* GENERATED_FIR_TAGS: isExpression, lambdaLiteral, propertyDeclaration, smartcast, whenExpression, whenWithSubject */
