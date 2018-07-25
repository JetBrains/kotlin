// IGNORE_BACKEND: JVM_IR
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

public inline fun <T> with2(receiver: T,  body: T.() -> String) =  receiver.body()

// FILE: 2.kt
import test.*

fun <T : Any> test(item: T?, defaultLink: T.() -> String): String {
    return with2("") {
        item?.defaultLink() ?: "fail"
    }
}

fun box(): String {
    return test("O") {
        this + "K"
    }
}