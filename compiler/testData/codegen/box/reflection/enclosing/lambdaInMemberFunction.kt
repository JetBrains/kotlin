// TARGET_BACKEND: JVM
// LAMBDAS: CLASS
// WITH_REFLECT

package test

class C {
    fun foo(): Any {
        return {}
    }
}


fun box(): String {
    val javaClass = C().foo().javaClass
    val enclosingMethod = javaClass.getEnclosingMethod()
    if (enclosingMethod?.getName() != "foo") return "method: $enclosingMethod"

    val enclosingClass = javaClass.getEnclosingClass()
    if (enclosingClass?.getName() != "test.C") return "enclosing class: $enclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}
