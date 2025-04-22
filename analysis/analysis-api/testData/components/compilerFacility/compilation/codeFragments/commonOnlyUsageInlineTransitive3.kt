// DUMP_IR
// LANGUAGE: +MultiPlatformProjects

// MODULE: base
// TARGET_PLATFORM: Common

// FILE: Base.kt
package base

inline fun base(): String = "base"


// MODULE: common(base)
// TARGET_PLATFORM: Common

// FILE: Common.kt
package test
import base.*

inline fun lib(): String = base()


// MODULE: app(common, base)
// TARGET_PLATFORM: JVM

// FILE: Jvm.kt
package app
import test.*

fun test() {
    <caret_context>lib()
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: app

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
lib()