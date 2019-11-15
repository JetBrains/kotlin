// !LANGUAGE: +MultiPlatformProjects
// FILE: impl.kt
class A(val result: String = "OK")

// FILE: multiplatform.kt
expect class B(result: String = "FAIL")

actual typealias B = A

fun box(): String {
    return B().result
}
