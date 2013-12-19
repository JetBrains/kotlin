package f

object A {
    class LoginFormPage() : Request({
        val failed = session.get("LOGIN_FAILED")
    })
}

class B {
    class object {
        class LoginFormPage() : Request({
            val failed = session.get("LOGIN_FAILED")
        })
    }

    class C {
        class LoginFormPage() : Request({
            val failed = session.get("LOGIN_FAILED")
        })
    }
}

open class Request(private val handler: ActionContext.() -> Unit) {}

trait ActionContext {
    val session : Map<String, String>
}