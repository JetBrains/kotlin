open class C(val a: Any)

fun box(): String {
    val l = object : C({}) {
    }

    val javaClass = l.a.javaClass
    if (javaClass.getEnclosingConstructor() != null) return "ctor should be null"

    val enclosingMethod = javaClass.getEnclosingMethod()!!.getName()
    if (enclosingMethod != "box") return "method: $enclosingMethod"

    val enclosingClass = javaClass.getEnclosingClass()!!.getName()
    if (!enclosingClass.startsWith("_DefaultPackage\$") || enclosingClass != l.javaClass.getEnclosingClass()!!.getName())
        return "enclosing class: $enclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}
