// TARGET_BACKEND: JVM
// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// FILE: 1.kt

package test

inline fun test(s: () -> Unit) {
    s()
}

// FILE: 2.kt

import test.*

fun box(): String {
    var s1 = ""
    var s2 = ""
    test {
        {
            val p = object {}
            // Check that Java reflection doesn't crash. Actual values are tested in bytecodeListing/inline/enclosingInfo/.
            s1 = p.javaClass.enclosingMethod.declaringClass.toString();
            {
                val q = object {}
                s2 = q.javaClass.enclosingMethod.declaringClass.toString()
            }()
        }()
    }

    return "OK"
}
