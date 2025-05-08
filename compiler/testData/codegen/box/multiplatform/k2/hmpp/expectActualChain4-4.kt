// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

// MODULE: lib-common
// FILE: libCommon.kt
expect class LibClass {
    val info: String
}

// MODULE: lib-inter1()()(lib-common)
// FILE: libInter1.kt

// MODULE: lib-inter2()()(lib-common)
// FILE: libInter2.kt

// MODULE: lib-platform()()(lib-inter1, lib-inter2)
// FILE: libPlatform.kt
actual class LibClass(actual val info: String = "OK")

// MODULE: app-common(lib-common)
// FILE: appCommon.kt
expect class AppInterClass {
    val info: String
}

expect class AppClass {
    val info: String
}

// MODULE: app-inter1(lib-inter1)()(app-common)
// FILE: appInter1.kt
actual typealias AppClass = AppInterClass

// MODULE: app-inter2(lib-inter2)()(app-common)
// FILE: appInter2.kt
actual typealias AppInterClass = LibClass

// MODULE: app-platform(lib-platform)()(app-inter1, app-inter2)
// FILE: appPlatform.kt
fun box(): String = AppClass().info