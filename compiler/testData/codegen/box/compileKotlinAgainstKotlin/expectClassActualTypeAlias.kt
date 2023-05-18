// IGNORE_BACKEND: NATIVE
// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE
// FIR status: In FIR, declaring the same `expect` and `actual` classes in one compiler module is not possible (see KT-55177).

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
