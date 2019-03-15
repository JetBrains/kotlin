// IGNORE_BACKEND: JVM_IR
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
    test {
        {
            val p = object {}
            encl1 = p.javaClass.enclosingMethod.declaringClass.name
            {

                val p = object {}
                encl2 = p.javaClass.enclosingMethod.declaringClass.name
            }()
        }()
    }

    if (encl1 != "_2Kt\$box\$\$inlined\$test\$lambda$1") return "fail 1: $encl1"
    if (encl2 != "_2Kt\$box\$\$inlined\$test\$lambda$1$2") return "fail 2: $encl2"

    return "OK"
}
