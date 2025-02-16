// LANGUAGE: +MultiPlatformProjects, +ValueClasses
// IGNORE_BACKEND_K1: ANY
// ^^^ kotlin.jvm.JvmInline is not accessible in common sources
// ISSUE: KT-71656
// WITH_STDLIB

// MODULE: common
// FILE: expect.kt
@kotlin.jvm.JvmInline
value class Wrapper(val value: Int)

@kotlin.jvm.JvmInline
value class Color(val value: Wrapper) {
    fun copy(arg: Int = 0): Color = Color(Wrapper(arg))
    override fun toString(): String = ""
}

// MODULE: main()()(common)
// FILE: actual.kt
import Color
import Wrapper

fun box(): String {
    "bla" is Wrapper
    Color(Wrapper(0)).copy()
    return "OK"
}
