// RUN_PIPELINE_TILL: BACKEND
inline fun <L> runLogged(action: () -> L): L {
    return action()
}

operator fun String.getValue(receiver: Any?, p: Any): String =
    runLogged { this }

val testK by runLogged { "K" }

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, inline, lambdaLiteral,
nullableType, operator, propertyDeclaration, propertyDelegate, stringLiteral, thisExpression, typeParameter */
