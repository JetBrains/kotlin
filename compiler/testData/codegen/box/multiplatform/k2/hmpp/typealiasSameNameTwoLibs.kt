// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib1-common
typealias Some = String

fun lib1Fun(x: Some): String = "lib1$x"

// MODULE: lib1-platform()()(lib1-common)
fun lib1PlatformFun(x: Some): String = "lib1Platform$x"

// MODULE: lib2-common
typealias Some = String

fun lib2Fun(x: Some): String = "lib2$x"

// MODULE: lib2-platform()()(lib2-common)
fun lib2PlatformFun(x: Some): String = "lib2Platform$x"

// MODULE: app-common(lib1-common, lib2-common)
fun appCommonFun(x1: Some, x2: Some): String {
    return lib1Fun(x1) + lib2Fun(x2)
}

// MODULE: app-platform(lib1-platform, lib2-platform)()(app-common)
fun appPlatformFun(x: Some): String {
    val a = lib1PlatformFun(x)
    val b = lib2PlatformFun(x)
    return a + b
}

fun box(): String {
    val x: Some = "OK"
    val fromCommon = appCommonFun(x, x)
    val fromPlatform = appPlatformFun(x)
    if (fromCommon != "lib1OKlib2OK") return "FAIL"
    if (fromPlatform != "lib1PlatformOKlib2PlatformOK") return "FAIL"
    return "OK"
}
