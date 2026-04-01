// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib-common
expect class LibString

typealias Some = LibString

fun libCommonFun(x: Some): String = "lib${x.toString()}"

// MODULE: lib-inter()()(lib-common)
typealias LibInterSome = Some

fun libInterFun(x: LibInterSome): String = "libInter${x.toString()}"

// MODULE: lib-platform()()(lib-inter)
actual typealias LibString = String

typealias LibPlatformSome = LibInterSome

fun libPlatformFun(x: LibPlatformSome): String = "libPlatform$x"

// MODULE: app-common(lib-common)
typealias Some = LibString

fun appCommonFun(x: Some): String = "app$x"

fun appCommonUse(x: Some): String = libCommonFun(x) + appCommonFun(x)

// MODULE: app-inter(lib-inter)(lib-common)(app-common)
fun appInterUse(x: Some): String {
    val fromCommon = appCommonUse(x)
    val fromLibInter = libInterFun(x)
    return "$fromCommon$fromLibInter"
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
    return if (r == "libOKappOKlibInterOKlibPlatformOK") "OK" else "FAIL"
}
