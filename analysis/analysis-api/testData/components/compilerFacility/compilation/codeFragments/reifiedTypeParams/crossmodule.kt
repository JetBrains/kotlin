// DUMP_CODE

// MODULE: jvm-lib-one
// TARGET_PLATFORM: JVM

// FILE: foo1.kt

inline fun <reified T1, reified T2> foo1() {
    <caret_context>val x = 1
}

// MODULE: jvm-lib-two()(jvm-lib-one)
// TARGET_PLATFORM: JVM

// FILE: foo2.kt

inline fun <reified T1> foo2() {
    <caret_stack_0>foo1<String, Array<T1>>()
}

// MODULE: jvm-app()(jvm-lib-two)
// TARGET_PLATFORM: JVM

// FILE: call.kt

fun main() {
    //Breakpoint!
    <caret_stack_1>foo2<Int>()
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: jvm-lib-one

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
T1::class.toString() + "_" + T2::class.toString()

