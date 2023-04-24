// TARGET_BACKEND: JVM
// LAMBDAS: CLASS
// WITH_REFLECT

fun box(): String {
    val l = {
        {}
    }

    val javaClass = l().javaClass
    val enclosingMethod = javaClass.getEnclosingMethod()
    if (enclosingMethod?.getName() != "invoke") return "method: $enclosingMethod"

    val enclosingClass = javaClass.getEnclosingClass()!!.getName()
    if (enclosingClass != "LambdaInLambdaKt\$box\$l\$1") return "enclosing class: $enclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}
