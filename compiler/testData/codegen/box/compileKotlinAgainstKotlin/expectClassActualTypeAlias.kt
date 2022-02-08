// IGNORE_BACKEND: NATIVE
// !LANGUAGE: +MultiPlatformProjects
// MODULE: lib
// FILE: impl.kt
class A(val result: String = "OK")

// MODULE: main(lib)
// FILE: multiplatform.kt
expect class B(result: String = "FAIL")

actual typealias B = A

fun box(): String {
    return B().result
}
