// TARGET_BACKEND: JVM
// LANGUAGE: +CompanionBlocks +CompanionExtensions

// MODULE: lib
// FILE: lib.kt
// EXTERNAL_FILE

package org.example.lib

class C

companion fun C.exampleFun() {}

companion val C.exampleVal: Int get() = 42

// MODULE: test(lib)
// FILE: companionExtension.kt

import org.example.lib.C
import org.example.lib.exampleFun
import org.example.lib.exampleVal

fun test() {
    C.exampleFun()
    val value = C.exampleVal
}
