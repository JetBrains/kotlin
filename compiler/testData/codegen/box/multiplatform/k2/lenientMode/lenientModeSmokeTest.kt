// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY
// MODULE: common
// FILE: common.kt
package pkg

expect class Speaker constructor() {
    fun sayK(): String
}

expect class Mime constructor() {
    fun sayNothing(): String
}

expect fun doNothing()

expect val unitProp: Unit

expect fun sayO(): String

// MODULE: jvm()()(common)
// FILE: jvm.kt
package pkg

actual class Speaker {
    actual fun sayK() = "K"
}

actual fun sayO() = "O"

fun box(): String {
    unitProp
    doNothing()
    return sayO() + Speaker().sayK() + Mime().sayNothing()
}
