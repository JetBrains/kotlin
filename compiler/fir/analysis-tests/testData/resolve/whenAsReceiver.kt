// RUN_PIPELINE_TILL: BACKEND
fun <T, R> T.also(block: () -> R): R {
    return null!!
}

fun foo(b: Boolean, a: Int) {
    val x = when (b) {
        true -> a
        else -> null
    }?.also {
        1
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, equalityExpression, funWithExtensionReceiver, functionDeclaration,
functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration, safeCall, typeParameter,
whenExpression, whenWithSubject */
