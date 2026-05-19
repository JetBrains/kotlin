// LANGUAGE: +MultiPlatformProjects

// MODULE: commonMain
// FILE: Common.kt

package sample
expect fun foo()

// MODULE: androidMain()()(commonMain)
// FILE: JvmAndroid.kt

package sample
<expr>internal actual fun foo() {}</expr>
