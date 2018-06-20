// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

class C {
    fun foo(): Any {
        return {}
    }
}


fun box(): String {
    val javaClass = C().foo().javaClass
    val enclosingMethod = javaClass.getEnclosingMethod()
    if (enclosingMethod?.getName() != "foo") return "method: $enclosingMethod"

    val enclosingClass = javaClass.getEnclosingClass()
    if (enclosingClass?.getName() != "C") return "enclosing class: $enclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}
