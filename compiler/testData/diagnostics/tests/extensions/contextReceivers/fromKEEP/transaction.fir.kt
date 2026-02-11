// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

class Session(var lastAccess: Any?)
interface Transaction {
    fun loadSession(): Session
    fun storeSession(session: Session)
}

fun now(): Any? = null

context(Transaction)
fun updateUserSession() {
    val session = <!UNRESOLVED_REFERENCE!>loadSession<!>()
    session.lastAccess = now()
    <!UNRESOLVED_REFERENCE!>storeSession<!>(session)
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionDeclarationWithContext,
interfaceDeclaration, localProperty, nullableType, primaryConstructor, propertyDeclaration */
