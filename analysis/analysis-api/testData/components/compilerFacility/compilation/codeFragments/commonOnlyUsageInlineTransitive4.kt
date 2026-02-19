// DUMP_IR
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: Common.kt
package test

expect inline fun lib(): String


// MODULE: root
// TARGET_PLATFORM: JVM

// FILE: Root.kt
package root

inline fun root(): String = "root"


// MODULE: jvm(root)()(common)
// TARGET_PLATFORM: JVM

// FILE: Jvm.kt
package test
import root.*

actual inline fun lib(): String = root()


// MODULE: app(common)
// TARGET_PLATFORM: Common

// FILE: Common.kt
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
lib() + root.root()