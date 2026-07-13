// ISSUE: KT-76655 Kotlin/Native: Native test infrastructure does not support per-module directives
// LANGUAGE: +CompanionBlocksAndExtensions
// ^ On Native `CompanionBlocksAndExtensions` language feature enables the JVM-like initialization.
//   See nativeCompanionInitOrderCrossModuleLegacy for Native behavior without the language feature.
//   Native also needs tests when some modules are built with the feature enabled, and some with it disabled.
// ISSUE: KT-84267 K/Wasm: init order of companion objects is different from JVM
// ISSUE: KT-86640 K/Wasm: cross-module signature mismatch for parent companion getInstance
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS,Wasm-js:2.3,2.4
// ^^^KT-84267 is fixed in 2.4.20-beta1 for Wasm
// ^^^KT-40768 is fixed in 2.4.20-beta1 for JS

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
    return if (result == "Root.Companion\nLeaf.Companion\n") "OK" else "fail: '$result'"
}
