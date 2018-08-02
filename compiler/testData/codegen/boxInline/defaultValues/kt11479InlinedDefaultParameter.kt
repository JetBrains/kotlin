// FILE: 1.kt
package test

inline fun log(lazyMessage: () -> Any?) {
    lazyMessage()
}

inline fun z(): Boolean {
    "zzz"
    return true
}

// FILE: 2.kt

import test.*

inline fun getOrCreate(
        z : Boolean = z(),
        s: () -> String
) {
    log { s() }
}


fun box(): String {
    var z = "fail"
    getOrCreate { z = "OK"; z }

    return z
}