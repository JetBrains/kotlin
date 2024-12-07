// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt

package test

expect class A

expect val A.x: A.()-> String

// MODULE: platform()()(common)
// FILE: platform.kt

package test

actual class A(val a: String)

actual val A.x : A.() -> String
    get() = fun A.(): String { return this@x.a + this.a }

fun box(): String {
    return A("O").x(A("K"))
}