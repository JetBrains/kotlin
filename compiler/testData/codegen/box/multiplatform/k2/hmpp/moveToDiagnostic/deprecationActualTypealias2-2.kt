// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common

expect class E()

fun commonUse(): E = E()

// MODULE: lib-platform()()(lib-common)

@Deprecated("", level = DeprecationLevel.WARNING)
class Impl

actual typealias E = Impl

// MODULE: app-common(lib-common)

fun appCommonUse(): E = E()

// MODULE: app-platform(lib-platform)()(app-common)

fun box(): String {
    val e1 = commonUse()
    val e2 = appCommonUse()
    return "OK"
}
