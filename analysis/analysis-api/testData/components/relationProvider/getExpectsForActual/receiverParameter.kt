// LANGUAGE: +MultiPlatformProjects
// MODULE: commonMain
// FILE: Common.kt

package sample
expect fun String.foo() {}

// MODULE: androidMain()()(commonMain)
// FILE: JvmAndroid.kt

package sample
actual fun Str<caret>ing.foo() {}