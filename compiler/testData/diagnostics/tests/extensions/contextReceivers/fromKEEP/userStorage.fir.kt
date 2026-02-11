// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

class Storage<T> {
    inner class Info

    fun info(name: String) = Info()
}
class User
class Logger {
    fun info(message: String) {}
}

context(Logger, Storage<User>)
fun userInfo(name: String): Storage<User>.Info? {
    this<!UNRESOLVED_LABEL!>@Logger<!>.info("Retrieving info about $name")
    return this<!UNRESOLVED_LABEL!>@Storage<!>.info(name)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, inner, nullableType,
stringLiteral, thisExpression, typeParameter */
