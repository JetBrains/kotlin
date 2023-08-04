// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: 1.kt

fun <T> doNothing() {}

// FILE: 2.kt

inline fun <reified T> bar(str: String): String {
    return object {
        fun foo(x: String): String {
            doNothing<Int>()
            return x
        }
    }.foo(str)
}

fun box(): String {
    return bar<Int>("O") + bar<Int>("K")
}