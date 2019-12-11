package f

object A {
    class LoginFormPage() : Request({
        val failed = <!UNRESOLVED_REFERENCE!>session<!>.<!UNRESOLVED_REFERENCE!>get<!>("LOGIN_FAILED")
    })
}

class B {
    companion object {
        class LoginFormPage() : Request({
            val failed = <!UNRESOLVED_REFERENCE!>session<!>.<!UNRESOLVED_REFERENCE!>get<!>("LOGIN_FAILED")
        })
    }

    class C {
        class LoginFormPage() : Request({
            val failed = <!UNRESOLVED_REFERENCE!>session<!>.<!UNRESOLVED_REFERENCE!>get<!>("LOGIN_FAILED")
        })
    }
}

open class Request(private val handler: ActionContext.() -> Unit) {}

interface ActionContext {
    val session : Map<String, String>
}