// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: Other.kt
package test

inline fun foo() {
    Foo().call()
}

fun bar() {}

// FILE: Common.kt
package test

fun test() {
    <caret_context>foo()
}

expect class Foo {
    fun call()
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: common

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
foo()


// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM

// FILE: Jvm.kt
package test

actual class Foo {
    fun call() {}
}