// LANGUAGE: +MultiPlatformProjects

// FILE: common.kt

package test

expect val v: String

expect val Char.extensionVal: String

expect var String.extensionVar: Char

// FILE: jvm.kt

package test

actual val v: String = ""

actual val Char.extensionVal: String
    get() = toString()

actual var String.extensionVar: Char
    get() = this[0]
    set(value) {}

fun box(): String =
    v + 'O'.extensionVal + "K".extensionVar
