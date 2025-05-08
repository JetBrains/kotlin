// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

// MODULE: lib-common
// FILE: libCommon.kt
expect class LibClass {
    val libData: String
}

// MODULE: lib-platform()()(lib-common)
// FILE: libPlatform.kt
actual class LibClass(actual val libData: String = "OK")

// MODULE: app-common(lib-common)
// FILE: appCommon.kt
expect class AppClass {
    val libData: String
}

// MODULE: app-inter(lib-common)()(app-common)
// FILE: appInter.kt
actual typealias AppClass = LibClass

// MODULE: app-platform(lib-platform)()(app-inter)
// FILE: appPlatform.kt
fun box(): String = AppClass().libData