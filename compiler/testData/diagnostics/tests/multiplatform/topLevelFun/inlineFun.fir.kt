// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

inline expect fun inlineFun()
expect fun nonInlineFun()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun inlineFun() { }
actual fun nonInlineFun() { }

// MODULE: m3-js()()(m1-common)
// FILE: js.kt

actual inline fun inlineFun() { }
actual inline fun nonInlineFun() { }
