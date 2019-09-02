// TARGET_BACKEND: JVM
// NO_CHECK_LAMBDA_INLINING
// WITH_RUNTIME
// FILE: 1.kt
package test

inline fun test(s: () -> Unit) {
    s()
}

// FILE: 2.kt

import test.*

fun box(): String {
    var encl1 = "fail";
    var encl2 = "fail";
    var lambda1: (() -> Unit)? = null
    var lambda2: (() -> Unit)? = null
    test {
        lambda1 = {
            val p = object {}
            encl1 = p.javaClass.enclosingMethod.declaringClass.name
            lambda2 = {

                val p = object {}
                encl2 = p.javaClass.enclosingMethod.declaringClass.name
            }
            lambda2!!()
        }
        lambda1!!()
    }

    if (encl1 != lambda1!!.javaClass.name) return "fail 1: $encl1 != ${lambda1!!.javaClass.name}"
    if (encl2 != lambda2!!.javaClass.name) return "fail 2: $encl2 != ${lambda2!!.javaClass.name}"

    return "OK"
}
