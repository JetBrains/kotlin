// MODULE: libCommon
// LANGUAGE: +MultiPlatformProjects

// TARGET_PLATFORM: Common
// FILE: LibCommon.kt
package lib

inline expect fun lib(): String


// MODULE: libJvm()()(libCommon)
// LANGUAGE: +MultiPlatformProjects

// TARGET_PLATFORM: JVM
// FILE: LibJvm.kt
package lib

inline actual fun lib(): String = "jvm"


// MODULE: libJs()()(libCommon)
// LANGUAGE: +MultiPlatformProjects

// TARGET_PLATFORM: JS
// FILE: LibJs.kt
package lib

inline actual fun lib(): String = "js"


// MODULE: main(libCommon, libJvm)
// TARGET_PLATFORM: JVM
// FILE: main.kt

import lib.lib

fun test() {
    <caret>lib()
}