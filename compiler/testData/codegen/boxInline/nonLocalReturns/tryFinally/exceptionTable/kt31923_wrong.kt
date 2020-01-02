// !LANGUAGE: -ProperFinally
// TARGET_BACKEND: JVM
// FILE: 1.kt

package test

var result = ""

inline fun inlineFun(block: (String)-> String) {
    try {
        try {
            result += block("lambda")
            return
        } catch (fail: Throwable) {
            result += " catch"
        }
    } finally {
        result += " finally"
        throw RuntimeException()
    }

}

fun test() {
    inlineFun {
        result += it
        return
    }
}

// FILE: 2.kt

import test.*


fun box(): String {
    try {
        test()
        return "fail: expected exception"
    } catch (e: RuntimeException) {

    }

    return if (result == "lambda finally catch finally") "OK" else "fail: $result"
}