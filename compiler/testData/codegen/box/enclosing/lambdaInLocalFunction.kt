// TARGET_BACKEND: JVM
// LAMBDAS: CLASS
// WITH_STDLIB

fun box(): String {
    fun foo(): Any {
        return {}
    }

    val javaClass = foo().javaClass

    val actualEnclosingMethod = javaClass.getEnclosingMethod()!!.getName()
    if (actualEnclosingMethod != "box\$foo") return "method: $actualEnclosingMethod"

    val actualEnclosingClass = javaClass.getEnclosingClass()!!.getName()
    if (actualEnclosingClass != "LambdaInLocalFunctionKt") return "enclosing class: $actualEnclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}
