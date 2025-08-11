// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// IGNORE_HMPP: JVM_IR
// ISSUE: KT-80051
// MODULE: app-common()
fun foo(): ArrayDeque<String>? = null

// MODULE: app-platform()()(app-common)
fun box(): String {
    foo()
    return "OK"
}