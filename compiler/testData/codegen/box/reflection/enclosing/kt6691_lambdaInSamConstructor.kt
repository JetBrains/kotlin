// TARGET_BACKEND: JVM
// SAM_CONVERSIONS: CLASS
//  ^ SAM-convertion classes created with LambdaMetafactory have 'enclosingMethod' and 'enclosingClass'

// WITH_REFLECT
package test

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
    if (enclosingMethod?.getName() != "run") return "enclosing method: $enclosingMethod"

    val enclosingClass = javaClass.getEnclosingClass()!!.getName()
    if (enclosingClass != "test.A\$prop\$1") return "enclosing class: $enclosingClass"

    val declaringClass = javaClass.getDeclaringClass()
    if (declaringClass != null) return "anonymous function has a declaring class: $declaringClass"

    return "OK"
}
