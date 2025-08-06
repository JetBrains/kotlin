// WITH_STDLIB
// LANGUAGE: +MultiPlatformProjects
// MODULE: app-common
fun foo(): String {
    doubleArrayOf()
    val char = 'a'
    "\\u00${char.code}"
    return "OK"
}

// MODULE: app-platform()()(app-common)
fun box() = foo()