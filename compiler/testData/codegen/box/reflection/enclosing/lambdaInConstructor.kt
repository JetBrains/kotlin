// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

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
