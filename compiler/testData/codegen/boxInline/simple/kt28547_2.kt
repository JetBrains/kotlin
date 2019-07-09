// FILE: 1.kt

package test

inline fun Double.run(body : Double.() -> String): String {
    return this.body()
}

// FILE: 2.kt

import test.*

fun box(): String {
    var captured = "fail"
    return 1.0.run {
        if (this == 1.0) {
             "OK"
        } else captured
    }

}
