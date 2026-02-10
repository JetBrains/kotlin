// KT-2470 another name mangling bug: kotlin.test.failsWith() gets generated to invalid JS
// FILE: lib.kt
package foo

public inline fun <reified T : Throwable> failsWith(block: () -> Any): T {
    try {
        block()
    }
    catch (e: Throwable) {
        if (e is T) return e
    }

    throw Exception("Should have failed")
}

// FILE: main.kt
package foo

fun box(): String {
    val a = failsWith<Exception> {
        throw Exception("OK")
    }

    return a.message!!
}
