// !LANGUAGE: +MultiPlatformProjects

// MODULE: main
// TARGET_PLATFORM: JS
// FILE: lib.kt
@file:JsExport
package lib

open class Base {
    val foo: String = "foo"
}

// FILE: main.kt
package main

import lib.*

<expr>class Impl : Base()</expr>