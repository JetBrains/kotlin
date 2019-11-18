// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

class C {
    val l: Any = {}
}

fun box(): String {
    val javaClass = C().l.javaClass
    val enclosingConstructor = javaClass.getEnclosingConstructor()
    if (enclosingConstructor?.getDeclaringClass()?.getName() != "C") return "ctor: $enclosingConstructor"

    val enclosingClass = javaClass.getEnclosingClass()
    if (enclosingClass?.getName() != "C") return "enclosing class: $enclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}
