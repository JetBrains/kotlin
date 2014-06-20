open class C(val a: Any)

fun box(): String {
    class L : C({}) {
    }
    val l = L()

    val javaClass = l.a.javaClass
    val enclosingMethod = javaClass.getEnclosingConstructor()
    if (enclosingMethod?.getName() != "_DefaultPackage\$box\$L") return "ctor: $enclosingMethod"

    val enclosingClass = javaClass.getEnclosingClass()
    if (!enclosingClass!!.getName().startsWith("_DefaultPackage\$box\$L")) return "enclosing class: $enclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}