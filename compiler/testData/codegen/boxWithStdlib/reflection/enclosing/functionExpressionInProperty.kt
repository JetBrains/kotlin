val property = fun () {}

fun box(): String {
    val javaClass = property.javaClass

    val enclosingMethod = javaClass.getEnclosingMethod()
    if (enclosingMethod != null) return "method: $enclosingMethod"

    val enclosingClass = javaClass.getEnclosingClass()!!.getName()
    if (!enclosingClass.startsWith("_DefaultPackage") || !enclosingClass.contains("functionExpressionInProperty")) return "enclosing class: $enclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}