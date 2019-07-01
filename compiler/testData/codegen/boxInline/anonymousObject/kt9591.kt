// NO_CHECK_LAMBDA_INLINING
// WITH_RUNTIME
// FILE: 1.kt
package test



inline fun inlineFun(p: () -> Unit) {
    p()
}

// FILE: 2.kt

import test.*

public fun box(): String {
    var z = "fail"
    inlineFun {
        val obj = object  {
            val _delegate by lazy {
                z = "OK"
            }
        }

        obj._delegate
    }

    return z;
}
