// WITH_STDLIB
// FILE: 1.kt

package test

class SceneContainer2() {

    inline fun pushTo(time: Long = 0.seconds, transition: String = "TR"): String {
        return "OK"
    }
}

inline val Number.seconds: Long get() = this.toLong()

// FILE: 2.kt

import test.*

fun box(): String {
    return SceneContainer2().pushTo(time = 0.2.seconds)
}
