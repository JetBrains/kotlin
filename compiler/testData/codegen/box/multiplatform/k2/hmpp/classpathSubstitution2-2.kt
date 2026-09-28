// LANGUAGE: +MultiPlatformProjects
// DONT_TARGET_EXACT_BACKEND: NATIVE
// ISSUE: KT-89466

// MODULE: lib-common
class Some

// MODULE: lib-platform()()(lib-common)

// MODULE: app-common(lib-common)
fun test(s: Some) {}

// MODULE: app-platform(lib-platform)()(app-common)
class Some

fun box(): String {
    return "OK"
}
