// FILE: 1.kt
//NO_CHECK_LAMBDA_INLINING

package test

object TimeUtil {
    inline fun waitForEx(retryWait: Int = 200,
                         action: () -> Boolean) {
        var now = 1L
        if (now++ <= 3) {
            action()
        }

    }

}

// FILE: 2.kt

import test.*

var result = "fail"

fun box(): String {
    TimeUtil.waitForEx(
            action = {
                try {
                    result = "OK"
                    true
                }
                catch (t: Throwable) {
                    false
                }
            })
    return result
}