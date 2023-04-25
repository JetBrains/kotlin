// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_MULTI_MODULE: ANY

// MODULE: lib1
// FILE: impl.kt
class A(val result: String = "OK")

// MODULE: lib2
// FILE: B.kt
expect class B(result: String = "FAIL")

// MODULE: main(lib1)()(lib2)
// FILE: multiplatform.kt
actual typealias B = A

fun box(): String {
    return B().result
}
