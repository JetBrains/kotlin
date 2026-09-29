// EMIT_JVM_TYPE_ANNOTATIONS
// JVM_DEFAULT_MODE: no-compatibility
// JVM_TARGET: 1.8
// RENDER_ANNOTATIONS
// WITH_STDLIB
// LANGUAGE: +JvmEnhancedBridges

// MODULE: lib
// FILE: lib.kt

package lib

import java.io.IOException
import java.io.FileNotFoundException

interface A<T> {
    @Throws(IOException::class)
    fun foo() : T
}

open class B<T : CharSequence> : A<T> {
    @Throws(FileNotFoundException::class)
    override fun foo() : T = throw FileNotFoundException()
}

// MODULE: main(lib)
// FILE: main.kt

package main

import lib.*

class J1: A<String> {
    override fun foo() : String = "OK"
}

class J2: B<String>() {
    override fun foo() : String = "OK"
}
