fun box(): String {

    val objectInLambda = {
        object : Any () {}
    }()

    val enclosingMethod = objectInLambda.javaClass.getEnclosingMethod()
    if (enclosingMethod?.getName() != "invoke") return "method: $enclosingMethod"

    val enclosingClass = objectInLambda.javaClass.getEnclosingClass()!!.getName()
    if (enclosingClass != "ObjectInLambdaKt\$box\$objectInLambda\$1") return "enclosing class: $enclosingClass"

    val declaringClass = objectInLambda.javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous object has a declaring class"

    return "OK"
}
