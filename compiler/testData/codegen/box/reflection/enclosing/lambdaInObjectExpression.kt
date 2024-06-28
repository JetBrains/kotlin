// TARGET_BACKEND: JVM
// LAMBDAS: CLASS
// WITH_REFLECT

interface C {
    val a: Any
}

fun box(): String {
    val l = object : C {
        override val a: Any

        init {
            a = {}
        }
    }

    val javaClass = l.a.javaClass
    val enclosingMethod = javaClass.getEnclosingConstructor()!!.getName()
    if (enclosingMethod != "LambdaInObjectExpressionKt\$box\$l\$1") return "ctor: $enclosingMethod"

    val enclosingClass = javaClass.getEnclosingClass()!!.getName()
    if (enclosingClass != "LambdaInObjectExpressionKt\$box\$l\$1") return "enclosing class: $enclosingClass"

    if (enclosingMethod != enclosingClass) return "$enclosingClass != $enclosingMethod"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}
