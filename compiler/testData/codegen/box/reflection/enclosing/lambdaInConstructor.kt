// TARGET_BACKEND: JVM
// LAMBDAS: CLASS
// WITH_REFLECT

package test

class C {
    val l: Any = {}
}

fun box(): String {
    val javaClass = C().l.javaClass
    val enclosingConstructor = javaClass.getEnclosingConstructor()
    if (enclosingConstructor?.getDeclaringClass()?.getName() != "test.C") return "ctor: $enclosingConstructor"

    val enclosingClass = javaClass.getEnclosingClass()
    if (enclosingClass?.getName() != "test.C") return "enclosing class: $enclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}
