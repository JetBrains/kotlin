// LANGUAGE: +MultiPlatformProjects, +ValueClasses
// IGNORE_BACKEND_K1: ANY
// ^^^ ULong is not accessible in common sources
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
// ISSUE: KT-71656
// WITH_STDLIB

// MODULE: common
// FILE: expect.kt

@kotlin.jvm.JvmInline
value class Color(val value: ULong) {
    fun copy(arg: Int = 0): Color = Color(arg.toULong())
    override fun toString(): String = ""
}

// MODULE: main()()(common)
// FILE: actual.kt

fun box(): String {
    "bla" is ULong
    Color(0UL).copy()
    return "OK"
}
