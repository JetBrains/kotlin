// See companionInitOrderWithSuperclassCrossModule for the common treatment
// TARGET_BACKEND: NATIVE
// LANGUAGE: -CompanionBlocks -CompanionExtensions
// Without this language feature, the initialization order on Native does not include recursive initialization of superclasses and superinterfaces.

// MODULE: lib
// FILE: lib.kt

package lib

var initLog = ""

fun log(t: String) {
    initLog += t + "\n"
}

open class Root {
    companion object { init { log("Root.Companion") } }
}

open class Middle : Root()

// MODULE: main(lib)
// FILE: main.kt

import lib.Middle
import lib.initLog

class Leaf : Middle() {
    companion object { init { lib.log("Leaf.Companion") } }
}

fun box(): String {
    initLog = ""
    Leaf
    val result = initLog
    return if (result == "Leaf.Companion\n") "OK" else "fail: '$result'"
}
