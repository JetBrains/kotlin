val l: Any
    get() = {}

fun box(): String {

    val enclosingMethod = l.javaClass.getEnclosingMethod()
    if (enclosingMethod?.getName() != "getL") return "method: $enclosingMethod"

    val enclosingClass = l.javaClass.getEnclosingClass()
    if (!enclosingClass!!.getName().startsWith("_DefaultPackage-lambdaInPropertyGetter-")) return "enclosing class: $enclosingClass"

    val declaringClass = l.javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}