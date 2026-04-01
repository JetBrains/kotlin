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

fun appCommonUse(x: Some): String = libCommonFun(x) + appCommonFun(x)

// MODULE: app-inter(lib-common)()(app-common)
fun appInterUse(x: Some): String {
    return appCommonUse(x)
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun appPlatformUse(x: Some): String {
    val fromInter = appInterUse(x)
    val fromLibPlatform = libPlatformFun(x)
    return "$fromInter$fromLibPlatform"
}

fun box(): String {
    val s: Some = "OK"
    val r = appPlatformUse(s)
    return if (r == "libOKappOKlibPlatformOK") "OK" else "FAIL"
}
