// LANGUAGE: +MultiPlatformProjects
// MODULE: lib-common

// MODULE: lib-inter()()(lib-common)
open class InterException(message: String?) : Exception(message)

// MODULE: lib-platform()()(lib-inter)

// MODULE: app-common(lib-common)
expect open class CommonException(message: String?) : Exception

class CommonExceptionInheritor(message: String?) : CommonException(message)

fun foo(e: CommonExceptionInheritor) {
    e.message
}

// MODULE: app-inter(lib-inter)()(app-common)
actual typealias CommonException = InterException

// MODULE: app-platform(lib-platform)()(app-inter)
fun box(): String {
    return "OK"
}