// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

var _l: Any = ""

var l: Any
    get() = _l
    set(v) {
        _l = {}
    }

fun box(): String {
    l = "" // to invoke the setter

    val enclosingMethod = l.javaClass.getEnclosingMethod()
    if (enclosingMethod?.getName() != "setL") return "method: $enclosingMethod"

    val enclosingClass = l.javaClass.getEnclosingClass()!!.getName()
    if (enclosingClass != "LambdaInPropertySetterKt") return "enclosing class: $enclosingClass"

    val declaringClass = l.javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}
