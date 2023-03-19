// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, NATIVE

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
