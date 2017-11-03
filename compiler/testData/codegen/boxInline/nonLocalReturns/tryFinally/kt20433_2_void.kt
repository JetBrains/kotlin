// FILE: 1.kt
//WITH_RUNTIME
package test

public inline fun <T, R> T.mylet(block: (T) -> R): R {
    return block(this)
}

// FILE: 2.kt
import test.*

var message = ""

fun foo(root: String) {
    try {
        return root.let { _ ->
            try {
                if (!random()) {
                    message += root
                    return
                }
                message += "fail $root"
            } catch (e: Exception) {
                message += "Exception $e"
            }
            "fail"
        }
    } finally {
        message += " Finally block"
    }
}

var fail = false

fun random() = fail

fun box(): String {
    foo("OK")
    if (message != "OK Finally block") return "fail 1: $message"

    message = ""
    fail = true
    foo("OK")
    if (message != "fail OK Finally block" ) return "fail 2: $message"

    return "OK"
}
