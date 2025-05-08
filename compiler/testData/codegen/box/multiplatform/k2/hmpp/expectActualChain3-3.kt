// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

// MODULE: lib-common
expect class LibClass {
    val value: String
}

// MODULE: lib-inter()()(lib-common)
expect class LibInterClass {
    val value: String
}

// MODULE: lib-platform()()(lib-inter)
actual class LibClass(actual val value: String = "OK")

actual typealias LibInterClass = LibClass

// MODULE: app-common(lib-common)
expect class AppClass {
    val value: String
}

// MODULE: app-inter(lib-common)()(app-common)
actual typealias AppClass = LibClass

// MODULE: app-platform(lib-platform)()(app-inter)
fun box(): String = AppClass().value