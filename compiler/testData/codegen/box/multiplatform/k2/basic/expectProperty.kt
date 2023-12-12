// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

package test

expect val v: String

expect val Char.extensionVal: String

expect var String.extensionVar: Char

// MODULE: platform()()(common)
// FILE: platform.kt

package test

actual val v: String = ""

actual val Char.extensionVal: String
    get() = toString()

actual var String.extensionVar: Char
    get() = this[0]
    set(value) {}

fun box(): String =
    v + 'O'.extensionVal + "K".extensionVar
