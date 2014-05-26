fun box(): String {
    val l: Any = {}

    val javaClass = l.javaClass
    val enclosingMethod = javaClass.getEnclosingMethod()
    if (enclosingMethod?.getName() != "box") return "method: $enclosingMethod"

    val enclosingClass = javaClass.getEnclosingClass()
    if (!enclosingClass!!.getName().startsWith("_DefaultPackage-lambdaInFunction-")) return "enclosing class: $enclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}