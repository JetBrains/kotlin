// !LANGUAGE: +MultiPlatformProjects
// MODULE: commonMain
// FILE: Common.kt

package sample
expect object Platform {
    val name: String
}

// MODULE: androidMain()()(commonMain)
// FILE: JvmAndroid.kt

package sample
actual object <caret>Platform {
    actual val name: String = "JVM"
}