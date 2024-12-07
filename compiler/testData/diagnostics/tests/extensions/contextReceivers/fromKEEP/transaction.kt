// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers

class Session(var lastAccess: Any?)
interface Transaction {
    fun loadSession(): Session
    fun storeSession(session: Session)
}

fun now(): Any? = null

context(Transaction)
fun updateUserSession() {
    val session = loadSession()
    session.lastAccess = now()
    storeSession(session)
}