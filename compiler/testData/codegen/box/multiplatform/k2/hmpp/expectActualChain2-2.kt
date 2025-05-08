// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

// MODULE: lib-common
// FILE: libCommon.kt

expect class A

// MODULE: lib-platform()()(lib-common)
// FILE: libPlatform.kt

expect class B
actual typealias A = B

// MODULE: app-common(lib-common)
// FILE: appCommon.kt


// MODULE: app-platform(lib-platform)()(app-common)
// FILE: appPlatform.kt

actual class B(val x: String = "OK") // ACTUAL_WITHOUT_EXPECT

fun box() = A().x