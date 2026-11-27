// LANGUAGE: +JvmOptimizedNullCastMessages +JvmOptimizedNullCastMessagesInInlineFunctions
// TARGET_BACKEND: JVM

// MODULE: lib
// FILE: lib.kt
package lib

inline fun <reified T : Any> Any?.cast(): T = this as T

// MODULE: main(lib)
// FILE: box.kt
import lib.cast

fun box(): String {
    val message = try {
        (null as Any?).cast<String>()
        return "No exception"
    } catch (e: NullPointerException) {
        e.message
    }

    return if (message == "null cannot be cast to non-null type kotlin.String") "OK" else "Unexpected message: $message"
}
