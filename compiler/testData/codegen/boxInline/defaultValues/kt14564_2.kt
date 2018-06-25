// FILE: 1.kt

package test

var result = "fail"

object TimeUtil {

    fun waitForAssert(z: String) {
        waitForEx(
                action = {
                    result = z
                    result
                })
    }

    inline fun waitForEx(retryWait: Int = 200,
                         action: () -> String) {
        var now = 1L
        now++
        action()
    }

}

// FILE: 2.kt

import test.*

fun box(): String {
    TimeUtil.waitForAssert("OK")
    return result
}
