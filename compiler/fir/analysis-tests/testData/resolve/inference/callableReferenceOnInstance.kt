// RUN_PIPELINE_TILL: BACKEND
interface KaptLogger {
    val isVerbose: Boolean

    fun warn(message: String)
    fun error(message: String)
}

fun test(logger: KaptLogger) {
    val func = if (logger.isVerbose)
        logger::warn
    else
        logger::error
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, ifExpression, interfaceDeclaration, localProperty,
propertyDeclaration */
