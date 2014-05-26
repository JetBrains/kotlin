fun box(): String {

    val classInLambda = {
        class Z {}
        Z()
    }()

    val enclosingMethod = classInLambda.javaClass.getEnclosingMethod()
    if (enclosingMethod?.getName() != "invoke") return "method: $enclosingMethod"

    val enclosingClass = classInLambda.javaClass.getEnclosingClass()
    if (enclosingClass?.getName() != "_DefaultPackage\$box\$classInLambda\$1") return "enclosing class: $enclosingClass"

    //KT-5092
    //val declaringClass = classInLambda.javaClass.getDeclaringClass()
    //if (declaringClass == null) return "class hasn't a declaring class"

    return "OK"
}