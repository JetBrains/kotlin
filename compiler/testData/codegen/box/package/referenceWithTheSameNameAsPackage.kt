// !LANGUAGE: +NewInference
// WITH_RUNTIME

// FILE: messages/foo.kt

package messages

fun foo() {}

// FILE: sample.kt

class Test {
    val messages = arrayListOf<String>()

    fun test(): Boolean {
        return messages.any { it == "foo" }
    }
}

fun box(): String {
    val result = Test().test()
    return if (result) "faile" else "OK"
}