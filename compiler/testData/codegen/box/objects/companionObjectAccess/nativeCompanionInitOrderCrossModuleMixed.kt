// See companionInitOrderWithSuperclassCrossModule for the common treatment
// TARGET_BACKEND: NATIVE

// MODULE: lib
// LANGUAGE: -CompanionBlocksAndExtensions
// ^ In this module the legacy Native initialization order is used
// FILE: lib.kt

package lib

var initLog = ""

fun log(t: String) {
    initLog += t + "\n"
}

open class Root {
    companion object { init { log("Root.Companion") } }
}

open class Middle : Root() {
    companion object { init { log("Middle.Companion") } }
}

// MODULE: main(lib)
// LANGUAGE: +CompanionBlocksAndExtensions
// ^ In this module the new Native initialization order is used
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
    return if (result == "Middle.Companion\nLeaf.Companion\n") "OK" else "fail: '$result'"
}
