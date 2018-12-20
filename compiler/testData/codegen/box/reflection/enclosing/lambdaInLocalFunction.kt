// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

fun box(): String {
    fun foo(): Any {
        return {}
    }

    val javaClass = foo().javaClass
    val enclosingMethod = javaClass.getEnclosingMethod()
    if (enclosingMethod?.getName() != "invoke") return "method: $enclosingMethod"

    val enclosingClass = javaClass.getEnclosingClass()!!.getName()
    if (enclosingClass != "LambdaInLocalFunctionKt\$box$1") return "enclosing class: $enclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}
