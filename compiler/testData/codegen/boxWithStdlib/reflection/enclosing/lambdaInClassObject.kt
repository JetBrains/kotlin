class O {
    default object {
        // Currently we consider <clinit> in class O as the enclosing method of this lambda,
        // so we write outer class = O and enclosing method = null
        val f = {}
    }
}

fun box(): String {
    val javaClass = O.f.javaClass

    val enclosingMethod = javaClass.getEnclosingMethod()
    if (enclosingMethod != null) return "method: $enclosingMethod"

    val enclosingConstructor = javaClass.getEnclosingConstructor()
    if (enclosingConstructor != null) return "constructor: $enclosingConstructor"

    val enclosingClass = javaClass.getEnclosingClass()
    if (enclosingClass?.getName() != "O") return "enclosing class: $enclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}
