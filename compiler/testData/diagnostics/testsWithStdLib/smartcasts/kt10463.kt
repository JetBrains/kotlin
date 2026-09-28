// RUN_PIPELINE_TILL: CODEGEN
val test: Int = listOf<Any>().map {
    when (it) {
        is Int -> it
        else -> throw AssertionError()
    }
}.sum()

/* GENERATED_FIR_TAGS: isExpression, lambdaLiteral, propertyDeclaration, smartcast, whenExpression, whenWithSubject */
