// TARGET_BACKEND: JVM
// LAMBDAS: CLASS
// WITH_REFLECT

fun box(): String {
    val lambda = {
        object : Any () {}
    }

    val objectInLambda = lambda()

    val enclosingMethod = objectInLambda.javaClass.getEnclosingMethod()
    if (enclosingMethod?.getName() != "invoke") return "method: $enclosingMethod"

    val enclosingClass = objectInLambda.javaClass.getEnclosingClass()!!.getName()
    if (enclosingClass != "ObjectInLambdaKt\$box\$lambda\$1") return "enclosing class: $enclosingClass"

    val declaringClass = objectInLambda.javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous object has a declaring class"

    return "OK"
}
