// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: Common.kt
package test

fun test(foo: Foo) {
    <caret_context>foo.call()
}

expect class Foo {
    fun call()
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: common

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
foo.call()


// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM

// FILE: Jvm.kt
package test

actual class Foo {
    fun call() {}
}