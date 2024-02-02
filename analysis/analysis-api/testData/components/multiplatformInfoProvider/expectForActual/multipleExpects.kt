// !LANGUAGE: +MultiPlatformProjects

// MODULE: commonMain1
// FILE: Common1.kt

package sample
expect fun foo()

// MODULE: commonMain2
// FILE: Common2.kt

package sample
expect fun foo()

// MODULE: androidMain(commonMain1, commonMain2)
// FILE: JvmAndroid.kt

package sample
actual fun f<caret>oo() {}