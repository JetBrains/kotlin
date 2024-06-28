// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// FULL_JDK
// TARGET_BACKEND: JVM_IR

// MODULE: common
// FILE: common.kt

val p = LinkedHashMap<Any, Any>()

// MODULE: platform()()(common)
// FILE: platform.kt

val q = object : LinkedHashMap<Any, Any>() {
    val s = size
}

fun box() : String {
    return "OK"
}