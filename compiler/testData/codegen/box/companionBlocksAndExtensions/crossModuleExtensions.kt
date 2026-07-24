// LANGUAGE: +CompanionBlocks +CompanionExtensions

// MODULE: lib
// FILE: lib.kt
// EXTERNAL_FILE

package org.example.lib

class C

companion fun C.exampleFun(): String = "O"

companion val C.exampleVal: String get() = "K"

// MODULE: test(lib)
// FILE: crossModuleExtensions.kt

import org.example.lib.C
import org.example.lib.exampleFun
import org.example.lib.exampleVal

fun box(): String {
    return C.exampleFun() + C.exampleVal
}
