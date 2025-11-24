// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: app-common()
fun foo(): ArrayDeque<String> = ArrayDeque()

// MODULE: app-platform()()(app-common)
fun ArrayDeque<String>.ext(): String {
    add("OK")
    return first()
}

fun box(): String {
    val d = foo()
    return d.ext()
}
