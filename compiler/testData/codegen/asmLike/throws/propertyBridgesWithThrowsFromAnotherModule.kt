// JVM_DEFAULT_MODE: no-compatibility
// JVM_TARGET: 1.8
// WITH_STDLIB
// LANGUAGE: +JvmEnhancedBridges

// MODULE: lib
// FILE: lib.kt

package lib

import java.io.IOException
import java.io.FileNotFoundException

interface A<T> {
    @get:Throws(IOException::class)
    @set:Throws(FileNotFoundException::class)
    var value: T
}

// MODULE: main(lib)
// FILE: main.kt

package main

import lib.A

class B : A<String> {
    override var value: String
        get() = "OK"
        set(value) {}
}
