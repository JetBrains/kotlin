// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

object O {
    val f = {}
}

fun box(): String {
    val javaClass = O.f.javaClass

    val enclosingMethod = javaClass.getEnclosingMethod()
    if (enclosingMethod != null) return "method: $enclosingMethod"

    val enclosingConstructor = javaClass.getEnclosingConstructor()
    if (enclosingConstructor != null) return "field should be initialized in clInit"

    val enclosingClass = javaClass.getEnclosingClass()
    if (enclosingClass?.getName() != "O") return "enclosing class: $enclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}
