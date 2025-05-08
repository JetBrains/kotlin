// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

// MODULE: lib-common
// FILE: libCommon.kt
expect class LibPlatformClass {
    val message: String
}

expect class LibClass {
    val message: String
}

// MODULE: lib-inter()()(lib-common)
// FILE: libInter.kt
expect class LibInterClass {
    val message: String
}

// MODULE: lib-platform()()(lib-inter)
// FILE: libPlatform.kt
actual class LibPlatformClass(actual val message: String = "OK")
actual typealias LibInterClass = LibPlatformClass
actual typealias LibClass = LibPlatformClass

// MODULE: app-common(lib-common)
// FILE: appCommon.kt
expect class AppClass {
    val message: String
}

expect class AppInterClass {
    val message: String
}

// MODULE: app-inter1(lib-common)()(app-common)
// FILE: appInter1.kt
actual typealias AppClass = AppInterClass

// MODULE: app-inter2(lib-common)()(app-common)
// FILE: appInter2.kt
actual typealias AppInterClass = LibPlatformClass


// MODULE: app-platform(lib-platform)()(app-inter1, app-inter2)
// FILE: appPlatform.kt
fun box(): String = AppClass().message
