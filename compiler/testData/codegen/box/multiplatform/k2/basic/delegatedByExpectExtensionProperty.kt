// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// MODULE: common
// FILE: common.kt

package test

expect open class A() {
    val a: String.() -> String
}

class B : A() {
    val b: (String) -> String by this::a
}

// MODULE: platform()()(common)
// FILE: platform.kt

package test

actual open class A {
    actual val a: String.() -> String = { this }
}

fun box(): String{
    return B().b("OK")
}