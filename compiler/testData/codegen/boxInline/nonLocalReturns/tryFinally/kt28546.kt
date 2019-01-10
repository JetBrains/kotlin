// IGNORE_BACKEND: JVM_IR
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

inline fun takeWhileSize(initialSize: Int , block: (String) -> Int) {
    val current = "PARAM"

    try {
        if (1 >= initialSize) {
            try {
                block(current)
            } finally {
                val i = "INNER FINALLY"
            }
        } else {
            val e = "ELSE"
        }
    } finally {
        val o =  "OUTER FINALLY"
    }
}

// FILE: 2.kt
import test.*


fun box(): String {
    takeWhileSize(1) {
        return "OK"
    }

    return "fail"
}