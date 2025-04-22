// DUMP_IR
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: Common.kt
package test

fun lib(): String = "foo"


// MODULE: jvm(common)
// TARGET_PLATFORM: JVM

// FILE: Jvm.kt
package test

fun test() {
    <caret_context>lib()
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: jvm

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
lib()