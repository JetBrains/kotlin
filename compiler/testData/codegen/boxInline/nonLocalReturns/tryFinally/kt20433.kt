// FILE: 1.kt
//WITH_RUNTIME
package test

public inline fun <T, R> T.mylet(block: (T) -> R): R {
    return block(this)
}

// FILE: 2.kt
import test.*

var message = ""

fun foo(root: String): String {
    try {
        return root.let { _ ->
            try {
                if (!random()) {
                    return root
                }
                return "fail $root"
            } catch (e: Exception) {
                message += "${e.message} "
            }
            "fail"
        }
    } finally {
        message += "Finally block"
    }
}

var exception = false

fun random() = if (exception) error("Exception") else false

fun box(): String {
    var okResult =  foo("OK")
    if (okResult != "OK") return "fail 1: $okResult"
    if (message != "Finally block") return "fail 2: $message"

    message = ""
    exception = true
    if (foo("OK") != "fail") return "fail 3: ${foo("OK")}"
    if (message != "Exception Finally block" ) return "fail 4: $message"

    return okResult
}
