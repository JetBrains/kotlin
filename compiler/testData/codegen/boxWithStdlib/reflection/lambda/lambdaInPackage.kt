val l: Any = {}

fun box(): String {
    val enclosingClass = l.javaClass.getEnclosingClass()
    if (!enclosingClass!!.getName().startsWith("_DefaultPackage-lambdaInPackage-")) return "enclosing class: $enclosingClass"

    val enclosingConstructor = l.javaClass.getEnclosingConstructor()
    if (enclosingConstructor != null) return "enclosing constructor found: $enclosingConstructor"

    val enclosingMethod = l.javaClass.getEnclosingMethod()
    if (enclosingMethod != null) return "enclosing method found: $enclosingMethod"

    val declaringClass = l.javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}