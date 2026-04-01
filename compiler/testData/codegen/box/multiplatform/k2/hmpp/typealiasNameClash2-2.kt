// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib-common
typealias Some = String

fun libCommonFun(x: Some): String = "lib$x"

// MODULE: lib-platform()()(lib-common)
typealias LibPlatformSome = Some

fun libPlatformFun(x: LibPlatformSome): String = "libPlatform$x"

// MODULE: app-common(lib-common)
typealias Some = String

fun appCommonFun(x: Some): String = "app$x"

fun useInCommon(x: Some): String = libCommonFun(x) + appCommonFun(x)

// MODULE: app-platform(lib-platform)()(app-common)
typealias AppPlatformSome = Some

fun useOnPlatform(x: AppPlatformSome): String {
    val fromCommon = useInCommon(x)
    val fromLibPlatform = libPlatformFun(x)
    return "$fromCommon$fromLibPlatform"
}

fun box(): String {
    val s: Some = "OK"
    val r = useOnPlatform(s)
    if (r != "libOKappOKlibPlatformOK") return "FAIL"
    return "OK"
}
