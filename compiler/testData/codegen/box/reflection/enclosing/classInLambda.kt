// TARGET_BACKEND: JVM
// LAMBDAS: CLASS
// WITH_REFLECT

fun box(): String {

    val lambda = {
        class Z {}
        Z()
    }

    val classInLambda = lambda()

    val enclosingMethod = classInLambda.javaClass.getEnclosingMethod()
    if (enclosingMethod?.getName() != "invoke") return "method: $enclosingMethod"

    val enclosingClass = classInLambda.javaClass.getEnclosingClass()!!.getName()
    if (enclosingClass != "ClassInLambdaKt\$box\$lambda\$1") return "enclosing class: $enclosingClass"

    val declaringClass = classInLambda.javaClass.getDeclaringClass()
    if (declaringClass != null) return "class has a declaring class"

    return "OK"
}
