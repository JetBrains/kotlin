// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib-common
expect class LibA {
    val t: String
}

fun LibA.libExt(): String = "libExt${this.t}"

// MODULE: lib-platform()()(lib-common)
actual class LibA constructor(
    actual val t: String
)

fun LibA.libPlatformExt(): String = "libPlatformExt${this.t}"

// MODULE: app-common(lib-common)
typealias AppA = LibA

fun AppA.appExt(): String = "appExt${this.t}"

fun appCommonUse(a: AppA): String = a.libExt() + a.appExt()

// MODULE: app-inter(lib-common)()(app-common)
fun appInterUse(a: AppA): String {
    return appCommonUse(a)
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun appPlatformUse(a: AppA): String {
    val fromInter = appInterUse(a)
    val fromPlatform = a.libPlatformExt()
    return "$fromInter$fromPlatform"
}

fun box(): String {
    val a = LibA("OK")
    val r = appPlatformUse(a)
    return if (r == "libExtOKappExtOKlibPlatformExtOK") "OK" else "FAIL"
}
