// IGNORE_BACKEND: JVM_IR
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
        root.let { _ ->
            try {
                if (!random()) {
                    message += root
                    return
                }
                message += "fail $root"
                return
            } catch (e: Exception) {
                message += "${e.message}"
            }
            "fail"
        }
    } finally {
        message += " Finally block"
    }
}

var exception = false

fun random() = if (exception) error("Exception") else false

fun box(): String {
    foo("OK")
    if (message != "OK Finally block") return "fail 1: $message"

    message = ""
    exception = true
    foo("OK")
    if (message != "Exception Finally block" ) return "fail 2: $message"

    return "OK"
}
