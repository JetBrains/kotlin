// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
package f

object A {
    class LoginFormPage() : Request({
        val failed = session.get("LOGIN_FAILED")
    })
}

class B {
    companion object {
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

interface ActionContext {
    val session : Map<String, String>
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionalType, interfaceDeclaration, lambdaLiteral,
localProperty, nestedClass, nullableType, objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral,
typeWithExtension */
