// TARGET_BACKEND: JVM
// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// IGNORE_BACKEND: ANDROID
// FILE: 1.kt

interface Flow<T> {
    val result: String
}

inline fun <reified T> foo() =
    object {
        fun test() = object : Flow<T> {
            override val result: String = "OK"
        }
    }.test()

// FILE: 2.kt

fun box(): String =
    foo<Any>().result
