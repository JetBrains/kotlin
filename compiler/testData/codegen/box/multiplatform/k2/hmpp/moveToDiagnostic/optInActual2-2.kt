// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class Ann

expect fun foo(): String


// MODULE: lib-platform()()(lib-common)
@Ann
actual fun foo(): String = "OK"

// MODULE: app-common(lib-common)

@OptIn(Ann::class)
fun withOptIn(): String {
    return foo()
}

// MODULE: app-platform(lib-platform)()(app-common)
fun box(): String {
    return withOptIn()
}
