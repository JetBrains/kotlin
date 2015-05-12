package f

object A {
    class LoginFormPage() : Request({
        val <!UNUSED_VARIABLE!>failed<!> = session.get("LOGIN_FAILED")
    })
}

class B {
    companion object {
        class LoginFormPage() : Request({
            val <!UNUSED_VARIABLE!>failed<!> = session.get("LOGIN_FAILED")
        })
    }

    class C {
        class LoginFormPage() : Request({
            val <!UNUSED_VARIABLE!>failed<!> = session.get("LOGIN_FAILED")
        })
    }
}

open class Request(private val handler: ActionContext.() -> Unit) {}

interface ActionContext {
    val session : Map<String, String>
}