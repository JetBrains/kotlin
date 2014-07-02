open class C(val a: Any)

fun box(): String {
    val l = object : C({}) {
    }

    val javaClass = l.a.javaClass
    val enclosingMethod = javaClass.getEnclosingConstructor()
    if (enclosingMethod?.getName() != "_DefaultPackage\$box\$l\$1") return "ctor: $enclosingMethod"

    val enclosingClass = javaClass.getEnclosingClass()
    if (!enclosingClass!!.getName().startsWith("_DefaultPackage\$box\$l\$1")) return "enclosing class: $enclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}