// TARGET_BACKEND: JVM
// LAMBDAS: CLASS
// WITH_REFLECT

val l: Any
    get() = {}

fun box(): String {

    val enclosingMethod = l.javaClass.getEnclosingMethod()
    if (enclosingMethod?.getName() != "getL") return "method: $enclosingMethod"

    val enclosingClass = l.javaClass.getEnclosingClass()!!.getName()
    if (enclosingClass != "LambdaInPropertyGetterKt") return "enclosing class: $enclosingClass"

    val declaringClass = l.javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}
