// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
fun foo(): Int {
    fun action(s: String): Int = s.toInt()

    val localAction = ::action

    return localAction("hello")
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, localFunction, localProperty, propertyDeclaration,
stringLiteral */
