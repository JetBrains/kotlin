// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

var lambda = {}

class A {
    val prop = Runnable {
        lambda = { println("") }
    }
}

fun box(): String {
    A().prop.run()

    val javaClass = lambda.javaClass

    val enclosingMethod = javaClass.getEnclosingMethod()
    if (enclosingMethod?.getName() != "run") return "method: $enclosingMethod"

    val enclosingClass = javaClass.getEnclosingClass()!!.getName()
    if (enclosingClass != "A\$prop\$1") return "enclosing class: $enclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}
