class Session(var lastAccess: Any?)
interface Transaction {
    fun loadSession(): Session
    fun storeSession(session: Session)
}

fun now(): Any? = null

context(Transaction)
fun updateUserSession() {
    val session = <!UNRESOLVED_REFERENCE!>loadSession<!>()
    session.<!UNRESOLVED_REFERENCE!>lastAccess<!> = now()
    <!UNRESOLVED_REFERENCE!>storeSession<!>(session)
}